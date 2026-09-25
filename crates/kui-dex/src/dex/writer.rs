use sha1::{Digest, Sha1};

/// Binary stream helper for writing little-endian Dalvik DEX structures.
pub struct DexWriter {
    buffer: Vec<u8>,
}

impl DexWriter {
    pub fn new() -> Self {
        Self { buffer: Vec::new() }
    }

    pub fn with_capacity(capacity: usize) -> Self {
        Self {
            buffer: Vec::with_capacity(capacity),
        }
    }

    pub fn len(&self) -> usize {
        self.buffer.len()
    }

    pub fn is_empty(&self) -> bool {
        self.buffer.is_empty()
    }

    pub fn write_u8(&mut self, val: u8) {
        self.buffer.push(val);
    }

    pub fn write_u16(&mut self, val: u16) {
        self.buffer.extend_from_slice(&val.to_le_bytes());
    }

    pub fn write_u32(&mut self, val: u32) {
        self.buffer.extend_from_slice(&val.to_le_bytes());
    }

    pub fn write_i32(&mut self, val: i32) {
        self.buffer.extend_from_slice(&val.to_le_bytes());
    }

    pub fn write_bytes(&mut self, bytes: &[u8]) {
        self.buffer.extend_from_slice(bytes);
    }

    pub fn align(&mut self, boundary: usize) {
        if boundary <= 1 {
            return;
        }
        let rem = self.buffer.len() % boundary;
        if rem != 0 {
            let pad = boundary - rem;
            self.buffer.resize(self.buffer.len() + pad, 0);
        }
    }

    pub fn into_bytes(self) -> Vec<u8> {
        self.buffer
    }

    pub fn as_slice(&self) -> &[u8] {
        &self.buffer
    }

    pub fn as_mut_slice(&mut self) -> &mut [u8] {
        &mut self.buffer
    }
}

/// Computes Adler-32 checksum according to RFC 1950.
pub fn compute_adler32(data: &[u8]) -> u32 {
    let mut a: u32 = 1;
    let mut b: u32 = 0;
    for &byte in data {
        a = (a + byte as u32) % 65521;
        b = (b + a) % 65521;
    }
    (b << 16) | a
}

/// Computes SHA-1 hash for bytes 32..end and Adler-32 checksum for bytes 12..end,
/// and patches them directly into the DEX header.
pub fn finalize_dex_header(raw: &mut [u8]) {
    assert!(raw.len() >= 112, "DEX file must be at least 112 bytes");

    // 1. SHA-1 Signature (bytes 12..31) computed over bytes 32..end
    let mut hasher = Sha1::new();
    hasher.update(&raw[32..]);
    let sha1 = hasher.finalize();
    raw[12..32].copy_from_slice(&sha1);

    // 2. Adler-32 Checksum (bytes 8..11) computed over bytes 12..end
    let checksum = compute_adler32(&raw[12..]);
    raw[8..12].copy_from_slice(&checksum.to_le_bytes());
}
