use super::digest::compute_apk_digest;
use super::v2_signer::{ApkV2Signer, APK_SIG_BLOCK_MAGIC, SIGNATURE_SCHEME_V2_BLOCK_ID};
use pkcs8::DecodePublicKey;
use rsa::pkcs1v15::VerifyingKey;
use rsa::signature::Verifier;
use rsa::RsaPublicKey;
use sha2::Sha256;

#[derive(Debug, PartialEq, Eq)]
pub struct VerificationSuccess {
    pub algorithm: String,
    pub certificate_der: Vec<u8>,
}

pub struct ApkV2Verifier;

impl ApkV2Verifier {
    pub fn verify(apk_bytes: &[u8]) -> Result<VerificationSuccess, String> {
        // 1. Locate EOCD
        let eocd_offset = ApkV2Signer::find_eocd_record(apk_bytes)
            .ok_or_else(|| "Missing End of Central Directory record".to_string())?;

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

        if cd_offset + cd_size > eocd_offset {
            return Err("Central Directory overlaps with or extends past EOCD".to_string());
        }

        // 2. Locate APK Signing Block immediately before Central Directory
        if cd_offset < 24 {
            return Err("APK file too small to contain APK Signing Block".to_string());
        }

        let magic_start = cd_offset - 16;
        if &apk_bytes[magic_start..cd_offset] != APK_SIG_BLOCK_MAGIC {
            return Err("APK Signing Block magic string not found before Central Directory".to_string());
        }

        let block_size2 = u64::from_le_bytes(
            apk_bytes[cd_offset - 24..cd_offset - 16]
                .try_into()
                .unwrap(),
        );

        let sig_block_start_signed = (cd_offset as i64) - 8 - (block_size2 as i64);
        if sig_block_start_signed < 0 || sig_block_start_signed > (cd_offset as i64) - 24 {
            return Err(format!("Invalid APK Signing Block size: {}", block_size2));
        }
        let sig_block_start = sig_block_start_signed as usize;

        let block_size1 = u64::from_le_bytes(
            apk_bytes[sig_block_start..sig_block_start + 8]
                .try_into()
                .unwrap(),
        );
        if block_size1 != block_size2 {
            return Err(format!(
                "APK Signing Block header size ({}) and footer size ({}) do not match",
                block_size1, block_size2
            ));
        }

        // 3. Scan ID-value pairs for APK Signature Scheme v2 (ID 0x7109871a)
        let mut pair_offset = sig_block_start + 8;
        let pairs_end = cd_offset - 24;
        let mut v2_block_value: Option<&[u8]> = None;

        while pair_offset < pairs_end {
            if pair_offset + 12 > pairs_end {
                return Err("Truncated ID-value pair in APK Signing Block".to_string());
            }
            let pair_len = u64::from_le_bytes(
                apk_bytes[pair_offset..pair_offset + 8]
                    .try_into()
                    .unwrap(),
            ) as usize;
            if pair_len < 4 || pair_offset + 8 + pair_len > pairs_end {
                return Err("Malformed ID-value pair in APK Signing Block".to_string());
            }
            let pair_id = u32::from_le_bytes(
                apk_bytes[pair_offset + 8..pair_offset + 12]
                    .try_into()
                    .unwrap(),
            );
            if pair_id == SIGNATURE_SCHEME_V2_BLOCK_ID {
                v2_block_value = Some(&apk_bytes[pair_offset + 12..pair_offset + 8 + pair_len]);
                break;
            }
            pair_offset += 8 + pair_len;
        }

        let v2_bytes = v2_block_value
            .ok_or_else(|| "APK Signature Scheme v2 block (ID 0x7109871a) not found".to_string())?;

        // 4. Parse Signer Block
        // signers: length-prefixed sequence
        if v2_bytes.len() < 4 {
            return Err("Malformed v2 value block: too short".to_string());
        }
        let signers_len = u32::from_le_bytes(v2_bytes[0..4].try_into().unwrap()) as usize;
        let signers_slice = &v2_bytes[4..4 + signers_len];

        // first signer: length-prefixed
        if signers_slice.len() < 4 {
            return Err("Malformed signers slice".to_string());
        }
        let signer_len = u32::from_le_bytes(signers_slice[0..4].try_into().unwrap()) as usize;
        let signer_slice = &signers_slice[4..4 + signer_len];

        let mut pos = 0;
        // signed_data
        let signed_data_len = u32::from_le_bytes(signer_slice[pos..pos + 4].try_into().unwrap()) as usize;
        pos += 4;
        let signed_data = &signer_slice[pos..pos + signed_data_len];
        pos += signed_data_len;

        // signatures list
        let sigs_len = u32::from_le_bytes(signer_slice[pos..pos + 4].try_into().unwrap()) as usize;
        pos += 4;
        let sigs_slice = &signer_slice[pos..pos + sigs_len];
        pos += sigs_len;

        // First signature entry
        let sig_entry_len = u32::from_le_bytes(sigs_slice[0..4].try_into().unwrap()) as usize;
        let sig_entry = &sigs_slice[4..4 + sig_entry_len];
        let _sig_algo = u32::from_le_bytes(sig_entry[0..4].try_into().unwrap());
        let sig_bytes_len = u32::from_le_bytes(sig_entry[4..8].try_into().unwrap()) as usize;
        let signature_bytes = &sig_entry[8..8 + sig_bytes_len];

        // public_key
        let pub_key_len = u32::from_le_bytes(signer_slice[pos..pos + 4].try_into().unwrap()) as usize;
        pos += 4;
        let pub_key_bytes = &signer_slice[pos..pos + pub_key_len];

        // Parse signed_data to extract expected digest and certificate
        let mut sd_pos = 0;
        // digests list
        let digests_len = u32::from_le_bytes(signed_data[sd_pos..sd_pos + 4].try_into().unwrap()) as usize;
        sd_pos += 4;
        let digests_slice = &signed_data[sd_pos..sd_pos + digests_len];
        sd_pos += digests_len;

        // first digest entry
        let digest_entry_len = u32::from_le_bytes(digests_slice[0..4].try_into().unwrap()) as usize;
        let digest_entry = &digests_slice[4..4 + digest_entry_len];
        let _digest_algo = u32::from_le_bytes(digest_entry[0..4].try_into().unwrap());
        let expected_digest_len = u32::from_le_bytes(digest_entry[4..8].try_into().unwrap()) as usize;
        let expected_digest = &digest_entry[8..8 + expected_digest_len];

        // certificates list
        let certs_len = u32::from_le_bytes(signed_data[sd_pos..sd_pos + 4].try_into().unwrap()) as usize;
        sd_pos += 4;
        let certs_slice = &signed_data[sd_pos..sd_pos + certs_len];
        let cert_len = u32::from_le_bytes(certs_slice[0..4].try_into().unwrap()) as usize;
        let cert_bytes = &certs_slice[4..4 + cert_len];

        // 5. Cryptographically verify signature over signed_data using public key
        let public_key = RsaPublicKey::from_public_key_der(pub_key_bytes)
            .map_err(|e| format!("Failed to parse RSA public key: {}", e))?;
        let verifying_key = VerifyingKey::<Sha256>::new(public_key);

        let signature = rsa::pkcs1v15::Signature::try_from(signature_bytes)
            .map_err(|e| format!("Invalid signature format: {}", e))?;

        verifying_key
            .verify(signed_data, &signature)
            .map_err(|_| "Cryptographic signature verification failed (corrupt or forged signature)".to_string())?;

        // 6. Recompute APK chunked tree digest across Section 1, Section 3, Section 4
        let section1 = &apk_bytes[0..sig_block_start];
        let section3 = &apk_bytes[cd_offset..cd_offset + cd_size];
        let mut section4 = apk_bytes[eocd_offset..].to_vec();

        // Temporarily reset CD offset in EOCD back to sig_block_start
        section4[16..20].copy_from_slice(&(sig_block_start as u32).to_le_bytes());

        let recomputed_digest = compute_apk_digest(&[section1, section3, &section4]);

        if expected_digest != recomputed_digest {
            return Err("APK content digest mismatch: Archive entries, Central Directory, or EOCD have been tampered with".to_string());
        }

        Ok(VerificationSuccess {
            algorithm: "SHA256withRSA (APK v2)".to_string(),
            certificate_der: cert_bytes.to_vec(),
        })
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::apk::ApkWriter;
    use std::collections::BTreeMap;

    #[test]
    fn test_sign_and_verify_cycle() {
        let manifest = b"binary xml data";
        let dex = b"dex\n035\0fake bytecode";
        let mut assets = BTreeMap::new();
        assets.insert("sub/asset.png".to_string(), b"fakepngdata".to_vec());

        let raw_apk = ApkWriter::build_apk(manifest, dex, &assets);
        assert!(ApkWriter::verify_alignment(&raw_apk));

        let signed_apk = ApkV2Signer::sign(&raw_apk, None).unwrap();
        assert!(signed_apk.len() > raw_apk.len());

        let result = ApkV2Verifier::verify(&signed_apk);
        assert!(result.is_ok(), "Verification failed: {:?}", result.err());

        // Tamper test: flip 1 byte in Section 1
        let mut tampered = signed_apk.clone();
        tampered[50] ^= 0xFF;
        let tampered_res = ApkV2Verifier::verify(&tampered);
        assert!(tampered_res.is_err(), "Tampered APK must fail verification!");
    }
}
