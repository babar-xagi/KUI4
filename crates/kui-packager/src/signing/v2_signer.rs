use super::digest::compute_apk_digest;
use super::key::{KeyManager, SigningConfig};
use rsa::pkcs1v15::SigningKey;
use rsa::signature::{SignatureEncoding, Signer};
use sha2::Sha256;
use std::fs;
use std::path::Path;

pub const APK_SIG_BLOCK_MAGIC: &[u8; 16] = b"APK Sig Block 42";
pub const SIGNATURE_SCHEME_V2_BLOCK_ID: u32 = 0x7109871a;
pub const SIGNATURE_RSA_PKCS1_V1_5_WITH_SHA256: u32 = 0x0103;

pub struct ApkV2Signer;

impl ApkV2Signer {
    pub fn sign(apk_bytes: &[u8], config: Option<&SigningConfig>) -> Result<Vec<u8>, String> {
        let default_config;
        let signing_config = match config {
            Some(c) => c,
            None => {
                default_config = KeyManager::get_or_create_debug_key()?;
                &default_config
            }
        };

        // 1. Locate End of Central Directory (EOCD)
        let eocd_offset = Self::find_eocd_record(apk_bytes)
            .ok_or_else(|| "Invalid ZIP archive: End of Central Directory not found".to_string())?;

        let cd_offset = u32::from_le_bytes(
            apk_bytes[eocd_offset + 16..eocd_offset + 20]
                .try_into()
                .unwrap(),
        ) as usize;
        let cd_size = u32::from_le_bytes(
            apk_bytes[eocd_offset + 12..eocd_offset + 16]
                .try_into()
                .unwrap(),
        ) as usize;

        // Verify ZIP integrity
        if cd_offset + cd_size != eocd_offset {
            return Err("ZIP Central Directory offset mismatch".to_string());
        }

        // Section 1: ZIP entries (from byte 0 to start of Central Directory)
        let section1 = &apk_bytes[0..cd_offset];
        // Section 3: Central Directory
        let section3 = &apk_bytes[cd_offset..cd_offset + cd_size];
        // Section 4: EOCD
        let section4 = &apk_bytes[eocd_offset..];

        // 2. Compute 1MB Chunked SHA-256 Digest
        let top_digest = compute_apk_digest(&[section1, section3, section4]);

        // 3. Build Signed Data
        let signed_data = Self::build_signed_data(&top_digest, &signing_config.certificate_der);

        // 4. Compute RSA-SHA256 Signature over Signed Data
        let signing_key = SigningKey::<Sha256>::new(signing_config.private_key.clone());
        let signature = signing_key.sign(&signed_data);
        let signature_bytes = signature.to_vec();

        // 5. Build APK Signature Scheme v2 Signer Block
        let signer_block = Self::build_signer_block(
            &signed_data,
            &signature_bytes,
            &signing_config.public_key_der,
        );

        let mut signers_seq = Vec::new();
        Self::write_length_prefixed(&mut signers_seq, &signer_block);

        let mut v2_value = Vec::new();
        Self::write_length_prefixed(&mut v2_value, &signers_seq);

        // 6. Build ID-Value Pair (ID 0x7109871a)
        let v2_block = Self::build_id_value_pair(SIGNATURE_SCHEME_V2_BLOCK_ID, &v2_value);

        // 7. Wrap into complete APK Signing Block
        let signing_block = Self::build_apk_signing_block(&[v2_block]);

        // 8. Update EOCD with new Central Directory Offset
        let new_cd_offset = (cd_offset + signing_block.len()) as u32;
        let mut updated_section4 = section4.to_vec();
        updated_section4[16..20].copy_from_slice(&new_cd_offset.to_le_bytes());

        // 9. Assemble final signed APK
        let mut final_apk = Vec::with_capacity(
            section1.len() + signing_block.len() + section3.len() + updated_section4.len(),
        );
        final_apk.extend_from_slice(section1);
        final_apk.extend_from_slice(&signing_block);
        final_apk.extend_from_slice(section3);
        final_apk.extend_from_slice(&updated_section4);

        Ok(final_apk)
    }

    pub fn sign_file(input_apk: &Path, output_apk: &Path, config: Option<&SigningConfig>) -> Result<(), String> {
        let raw_bytes = fs::read(input_apk).map_err(|e| format!("Failed to read input APK: {}", e))?;
        let signed_bytes = Self::sign(&raw_bytes, config)?;
        if let Some(parent) = output_apk.parent() {
            fs::create_dir_all(parent).map_err(|e| format!("Failed to create output dir: {}", e))?;
        }
        fs::write(output_apk, signed_bytes).map_err(|e| format!("Failed to write signed APK: {}", e))?;
        Ok(())
    }

    fn build_signed_data(top_digest: &[u8; 32], cert_der: &[u8]) -> Vec<u8> {
        let mut out = Vec::new();

        // 1. Digests list: sequence of length-prefixed digest entries
        let mut digest_entry = Vec::new();
        digest_entry.extend_from_slice(&SIGNATURE_RSA_PKCS1_V1_5_WITH_SHA256.to_le_bytes());
        Self::write_length_prefixed(&mut digest_entry, top_digest);

        let mut digests_list = Vec::new();
        Self::write_length_prefixed(&mut digests_list, &digest_entry);

        // 2. Certificates list: sequence of length-prefixed cert entries
        let mut certs_list = Vec::new();
        Self::write_length_prefixed(&mut certs_list, cert_der);

        // 3. Additional attributes: empty length-prefixed sequence
        let attrs_list = Vec::new();

        Self::write_length_prefixed(&mut out, &digests_list);
        Self::write_length_prefixed(&mut out, &certs_list);
        Self::write_length_prefixed(&mut out, &attrs_list);

        out
    }

    fn build_signer_block(
        signed_data: &[u8],
        signature_bytes: &[u8],
        public_key_der: &[u8],
    ) -> Vec<u8> {
        let mut out = Vec::new();

        // 1. Signed Data (length-prefixed)
        Self::write_length_prefixed(&mut out, signed_data);

        // 2. Signatures list (length-prefixed)
        let mut sig_entry = Vec::new();
        sig_entry.extend_from_slice(&SIGNATURE_RSA_PKCS1_V1_5_WITH_SHA256.to_le_bytes());
        Self::write_length_prefixed(&mut sig_entry, signature_bytes);

        let mut sigs_list = Vec::new();
        Self::write_length_prefixed(&mut sigs_list, &sig_entry);
        Self::write_length_prefixed(&mut out, &sigs_list);

        // 3. Public Key (length-prefixed)
        Self::write_length_prefixed(&mut out, public_key_der);

        out
    }

    fn build_id_value_pair(id: u32, value: &[u8]) -> Vec<u8> {
        let mut out = Vec::new();
        let pair_size = (4 + value.len()) as u64;
        out.extend_from_slice(&pair_size.to_le_bytes());
        out.extend_from_slice(&id.to_le_bytes());
        out.extend_from_slice(value);
        out
    }

    fn build_apk_signing_block(pairs: &[Vec<u8>]) -> Vec<u8> {
        let pairs_size: u64 = pairs.iter().map(|p| p.len() as u64).sum();

        // Total block size field excludes the first 8-byte size field itself:
        // blockSize = pairs_size + 8 (footer size) + 16 (magic)
        let block_size = pairs_size + 8 + 16;

        let mut out = Vec::new();
        out.extend_from_slice(&block_size.to_le_bytes());
        for p in pairs {
            out.extend_from_slice(p);
        }
        out.extend_from_slice(&block_size.to_le_bytes());
        out.extend_from_slice(APK_SIG_BLOCK_MAGIC);
        out
    }

    pub fn find_eocd_record(bytes: &[u8]) -> Option<usize> {
        if bytes.len() < 22 {
            return None;
        }
        let max_search = std::cmp::min(bytes.len(), 65536 + 22);
        let start = bytes.len() - 22;
        let min_idx = bytes.len() - max_search;

        for i in (min_idx..=start).rev() {
            if bytes[i] == 0x50
                && bytes[i + 1] == 0x4b
                && bytes[i + 2] == 0x05
                && bytes[i + 3] == 0x06
            {
                return Some(i);
            }
        }
        None
    }

    fn write_length_prefixed(out: &mut Vec<u8>, bytes: &[u8]) {
        out.extend_from_slice(&(bytes.len() as u32).to_le_bytes());
        out.extend_from_slice(bytes);
    }
}
