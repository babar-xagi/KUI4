use pkcs8::{DecodePrivateKey, EncodePrivateKey, EncodePublicKey};
use rand::rngs::OsRng;
use rsa::pkcs1v15::SigningKey;
use rsa::signature::{SignatureEncoding, Signer};
use rsa::{RsaPrivateKey, RsaPublicKey};
use sha2::Sha256;
use std::fs;
use std::path::PathBuf;
use std::time::{SystemTime, UNIX_EPOCH};

pub struct DerBuffer {
    data: Vec<u8>,
}

impl Default for DerBuffer {
    fn default() -> Self {
        Self::new()
    }
}

impl DerBuffer {
    pub fn new() -> Self {
        Self { data: Vec::new() }
    }

    pub fn write_raw(&mut self, bytes: &[u8]) {
        self.data.extend_from_slice(bytes);
    }

    pub fn write_length(&mut self, len: usize) {
        if len < 128 {
            self.data.push(len as u8);
        } else if len < 256 {
            self.data.push(0x81);
            self.data.push(len as u8);
        } else if len < 65536 {
            self.data.push(0x82);
            self.data.push((len >> 8) as u8);
            self.data.push((len & 0xFF) as u8);
        } else {
            self.data.push(0x83);
            self.data.push((len >> 16) as u8);
            self.data.push((len >> 8) as u8);
            self.data.push((len & 0xFF) as u8);
        }
    }

    pub fn to_sequence(&self) -> Vec<u8> {
        let mut seq = Vec::new();
        seq.push(0x30); // SEQUENCE tag
        let mut len_buf = DerBuffer::new();
        len_buf.write_length(self.data.len());
        seq.extend_from_slice(&len_buf.data);
        seq.extend_from_slice(&self.data);
        seq
    }

    pub fn write_integer(&mut self, val_bytes: &[u8]) {
        self.data.push(0x02); // INTEGER tag
        // If highest bit is 1, prepend 0x00 to mark as positive integer
        if !val_bytes.is_empty() && (val_bytes[0] & 0x80) != 0 {
            self.write_length(val_bytes.len() + 1);
            self.data.push(0x00);
        } else {
            self.write_length(val_bytes.len());
        }
        self.data.extend_from_slice(val_bytes);
    }

    pub fn write_u64_integer(&mut self, val: u64) {
        let be = val.to_be_bytes();
        let mut start = 0;
        while start < be.len() - 1 && be[start] == 0 {
            start += 1;
        }
        self.write_integer(&be[start..]);
    }

    pub fn write_bit_string(&mut self, bytes: &[u8]) {
        self.data.push(0x03); // BIT STRING tag
        self.write_length(bytes.len() + 1);
        self.data.push(0x00); // 0 unused bits
        self.data.extend_from_slice(bytes);
    }

    pub fn write_tagged_explicit(&mut self, tag: u8, content: &[u8]) {
        self.data.push(0xA0 | (tag & 0x1F));
        self.write_length(content.len());
        self.data.extend_from_slice(content);
    }

    pub fn write_sha256_with_rsa_algorithm_id(&mut self) {
        // AlgorithmIdentifier for sha256WithRSAEncryption (1.2.840.113549.1.1.11 with NULL param)
        // 30 0D 06 09 2A 86 48 86 F7 0D 01 01 0B 05 00
        const BYTES: [u8; 15] = [
            0x30, 0x0D, 0x06, 0x09, 0x2A, 0x86, 0x48, 0x86, 0xF7, 0x0D, 0x01, 0x01, 0x0B, 0x05,
            0x00,
        ];
        self.write_raw(&BYTES);
    }

    pub fn write_distinguished_name(&mut self, cn: &str) {
        // SEQUENCE of SET of SEQUENCE { OID 2.5.4.3 (commonName), UTF8String }
        let cn_bytes = cn.as_bytes();
        let mut atv = DerBuffer::new();
        atv.write_raw(&[0x06, 0x03, 0x55, 0x04, 0x03]); // OID 2.5.4.3
        atv.data.push(0x0C); // UTF8String tag
        atv.write_length(cn_bytes.len());
        atv.write_raw(cn_bytes);
        let atv_seq = atv.to_sequence();

        let mut rdn_set = Vec::new();
        rdn_set.push(0x31); // SET tag
        let mut set_len_buf = DerBuffer::new();
        set_len_buf.write_length(atv_seq.len());
        rdn_set.extend_from_slice(&set_len_buf.data);
        rdn_set.extend_from_slice(&atv_seq);

        let mut name_seq = Vec::new();
        name_seq.push(0x30); // SEQUENCE tag
        let mut name_len_buf = DerBuffer::new();
        name_len_buf.write_length(rdn_set.len());
        name_seq.extend_from_slice(&name_len_buf.data);
        name_seq.extend_from_slice(&rdn_set);

        self.write_raw(&name_seq);
    }

    pub fn write_validity(&mut self, not_before: &str, not_after: &str) {
        let nb_bytes = not_before.as_bytes();
        let na_bytes = not_after.as_bytes();

        let mut out = DerBuffer::new();
        out.data.push(0x17); // UTCTime
        out.write_length(nb_bytes.len());
        out.write_raw(nb_bytes);

        out.data.push(0x17); // UTCTime
        out.write_length(na_bytes.len());
        out.write_raw(na_bytes);

        let validity_seq = out.to_sequence();
        self.write_raw(&validity_seq);
    }
}

#[derive(Clone)]
pub struct SigningConfig {
    pub private_key: RsaPrivateKey,
    pub certificate_der: Vec<u8>,
    pub public_key_der: Vec<u8>,
}

pub struct KeyManager;

impl KeyManager {
    pub fn get_key_dir() -> PathBuf {
        let home = std::env::var("USERPROFILE")
            .or_else(|_| std::env::var("HOME"))
            .map(PathBuf::from)
            .unwrap_or_else(|_| PathBuf::from("."));
        home.join(".kui")
    }

    pub fn get_or_create_debug_key() -> Result<SigningConfig, String> {
        let dir = Self::get_key_dir();
        fs::create_dir_all(&dir).map_err(|e| format!("Failed to create ~/.kui dir: {}", e))?;

        let key_file = dir.join("debug.pk8");
        let cert_file = dir.join("debug.crt");

        if key_file.is_file() && cert_file.is_file() {
            if let Ok(key_bytes) = fs::read(&key_file) {
                if let Ok(cert_bytes) = fs::read(&cert_file) {
                    if let Ok(priv_key) = RsaPrivateKey::from_pkcs8_der(&key_bytes) {
                        let pub_key = RsaPublicKey::from(&priv_key);
                        if let Ok(pub_der) = pub_key.to_public_key_der() {
                            return Ok(SigningConfig {
                                private_key: priv_key,
                                certificate_der: cert_bytes,
                                public_key_der: pub_der.as_bytes().to_vec(),
                            });
                        }
                    }
                }
            }
        }

        // Generate new 2048-bit RSA key pair
        let mut rng = OsRng;
        let priv_key = RsaPrivateKey::new(&mut rng, 2048)
            .map_err(|e| format!("Failed to generate RSA 2048-bit key: {}", e))?;
        let pub_key = RsaPublicKey::from(&priv_key);

        let pub_der = pub_key
            .to_public_key_der()
            .map_err(|e| format!("Failed to encode public key to DER: {}", e))?
            .as_bytes()
            .to_vec();

        let cert_der = Self::generate_self_signed_certificate(&priv_key, &pub_der, "KUI Android Debug")?;

        let pkcs8_der = priv_key
            .to_pkcs8_der()
            .map_err(|e| format!("Failed to encode private key to PKCS8: {}", e))?;

        // Cache files
        fs::write(&key_file, pkcs8_der.as_bytes()).ok();
        fs::write(&cert_file, &cert_der).ok();

        Ok(SigningConfig {
            private_key: priv_key,
            certificate_der: cert_der,
            public_key_der: pub_der,
        })
    }

    pub fn generate_self_signed_certificate(
        priv_key: &RsaPrivateKey,
        pub_der: &[u8],
        subject_cn: &str,
    ) -> Result<Vec<u8>, String> {
        let now = SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .unwrap_or_default()
            .as_millis() as u64;

        // TBS Certificate structure
        let mut tbs = DerBuffer::new();

        // 1. Version: [0] EXPLICIT INTEGER 2 (v3)
        let mut ver_inner = DerBuffer::new();
        ver_inner.write_u64_integer(2);
        tbs.write_tagged_explicit(0, &ver_inner.data);

        // 2. Serial Number
        tbs.write_u64_integer(now);

        // 3. Signature Algorithm: sha256WithRSAEncryption
        tbs.write_sha256_with_rsa_algorithm_id();

        // 4. Issuer DN: CN=subject_cn
        tbs.write_distinguished_name(subject_cn);

        // 5. Validity: 10 years (e.g. 240101000000Z - 340101000000Z)
        tbs.write_validity("240101000000Z", "340101000000Z");

        // 6. Subject DN: same as issuer
        tbs.write_distinguished_name(subject_cn);

        // 7. SubjectPublicKeyInfo: public key DER
        tbs.write_raw(pub_der);

        let tbs_bytes = tbs.to_sequence();

        // Sign TBS with SHA256withRSA
        let signing_key = SigningKey::<Sha256>::new(priv_key.clone());
        let signature = signing_key.sign(&tbs_bytes);
        let sig_bytes = signature.to_vec();

        // Wrap into Certificate SEQUENCE
        let mut cert_out = DerBuffer::new();
        cert_out.write_raw(&tbs_bytes);
        cert_out.write_sha256_with_rsa_algorithm_id();
        cert_out.write_bit_string(&sig_bytes);

        Ok(cert_out.to_sequence())
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_generate_and_load_debug_key() {
        let temp_dir = tempfile::tempdir().unwrap();
        let _key_file = temp_dir.path().join("debug.pk8");
        let _cert_file = temp_dir.path().join("debug.crt");

        let mut rng = OsRng;
        let priv_key = RsaPrivateKey::new(&mut rng, 2048).unwrap();
        let pub_key = RsaPublicKey::from(&priv_key);
        let pub_der = pub_key.to_public_key_der().unwrap().as_bytes().to_vec();
        let cert_der = KeyManager::generate_self_signed_certificate(&priv_key, &pub_der, "Test Cert").unwrap();

        assert!(!cert_der.is_empty());
        assert_eq!(cert_der[0], 0x30); // SEQUENCE tag
    }
}
