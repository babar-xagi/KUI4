use sha2::{Digest, Sha256};

pub const CHUNK_SIZE: usize = 1_048_576; // 1 MB chunks

/// Computes the 2-level 1MB-chunked SHA-256 tree digest across sections of the APK.
///
/// Each chunk is hashed as:
///   chunk_digest = SHA-256( 0xa5 || 4-byte LE chunk_len || chunk_data )
///
/// The top-level digest is computed as:
///   top_digest = SHA-256( 0x5a || 4-byte LE chunk_count || concatenated_chunk_digests )
pub fn compute_apk_digest(sections: &[&[u8]]) -> [u8; 32] {
    let mut chunk_digests: Vec<[u8; 32]> = Vec::new();

    for section in sections {
        let mut offset = 0;
        while offset < section.len() {
            let chunk_size = std::cmp::min(CHUNK_SIZE, section.len() - offset);
            let chunk_data = &section[offset..offset + chunk_size];

            let mut hasher = Sha256::new();
            hasher.update([0xa5]);
            hasher.update((chunk_size as u32).to_le_bytes());
            hasher.update(chunk_data);

            let digest: [u8; 32] = hasher.finalize().into();
            chunk_digests.push(digest);

            offset += chunk_size;
        }
    }

    let mut top_hasher = Sha256::new();
    top_hasher.update([0x5a]);
    top_hasher.update((chunk_digests.len() as u32).to_le_bytes());
    for cd in &chunk_digests {
        top_hasher.update(cd);
    }

    top_hasher.finalize().into()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_chunked_digest_deterministic() {
        let data1 = vec![0x11u8; 200];
        let data2 = vec![0x22u8; 500];
        let digest1 = compute_apk_digest(&[&data1, &data2]);
        let digest2 = compute_apk_digest(&[&data1, &data2]);
        assert_eq!(digest1, digest2);
    }
}
