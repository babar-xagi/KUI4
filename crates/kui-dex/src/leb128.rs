/// Pure Rust implementation of LEB128 (Little-Endian Base 128) encoding
/// and decoding per Dalvik Executable (DEX) specification.

/// Writes an unsigned integer as ULEB128 to a buffer.
pub fn write_uleb128(buf: &mut Vec<u8>, mut val: u32) {
    loop {
        let byte = (val & 0x7F) as u8;
        val >>= 7;
        if val == 0 {
            buf.push(byte);
            break;
        } else {
            buf.push(byte | 0x80);
        }
    }
}

/// Reads an unsigned integer as ULEB128 from a byte slice.
/// Returns (value, bytes_consumed).
pub fn read_uleb128(bytes: &[u8]) -> Result<(u32, usize), String> {
    let mut result: u32 = 0;
    let mut shift: u32 = 0;
    let mut count = 0;

    for &b in bytes {
        count += 1;
        result |= ((b & 0x7F) as u32) << shift;
        if (b & 0x80) == 0 {
            return Ok((result, count));
        }
        shift += 7;
        if shift > 35 {
            return Err("ULEB128 overflow (exceeds 32-bit integer)".to_string());
        }
    }
    Err("Unexpected end of data reading ULEB128".to_string())
}

/// Writes a signed integer as SLEB128 to a buffer.
pub fn write_sleb128(buf: &mut Vec<u8>, mut val: i32) {
    let mut more = true;
    while more {
        let mut byte = (val & 0x7F) as u8;
        val >>= 7;
        if (val == 0 && (byte & 0x40) == 0) || (val == -1 && (byte & 0x40) != 0) {
            more = false;
        } else {
            byte |= 0x80;
        }
        buf.push(byte);
    }
}

/// Reads a signed integer as SLEB128 from a byte slice.
/// Returns (value, bytes_consumed).
pub fn read_sleb128(bytes: &[u8]) -> Result<(i32, usize), String> {
    let mut result: i32 = 0;
    let mut shift: u32 = 0;
    let mut count = 0;

    for &b in bytes {
        count += 1;
        result |= ((b & 0x7F) as i32) << shift;
        shift += 7;
        if (b & 0x80) == 0 {
            if shift < 32 && (b & 0x40) != 0 {
                result |= !0 << shift;
            }
            return Ok((result, count));
        }
        if shift > 35 {
            return Err("SLEB128 overflow (exceeds 32-bit integer)".to_string());
        }
    }
    Err("Unexpected end of data reading SLEB128".to_string())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_uleb128_roundtrip() {
        let test_cases = [0u32, 1, 127, 128, 255, 256, 16383, 16384, 0x123456, 0x7FFFFFFF];
        for &val in &test_cases {
            let mut buf = Vec::new();
            write_uleb128(&mut buf, val);
            let (decoded, count) = read_uleb128(&buf).unwrap();
            assert_eq!(decoded, val);
            assert_eq!(count, buf.len());
        }
    }

    #[test]
    fn test_sleb128_roundtrip() {
        let test_cases = [0i32, 1, -1, 63, -64, 64, -65, 127, -128, 128, -129, 0x123456, -0x123456];
        for &val in &test_cases {
            let mut buf = Vec::new();
            write_sleb128(&mut buf, val);
            let (decoded, count) = read_sleb128(&buf).unwrap();
            assert_eq!(decoded, val);
            assert_eq!(count, buf.len());
        }
    }
}
