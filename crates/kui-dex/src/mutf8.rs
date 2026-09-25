/// Pure Rust implementation of Modified UTF-8 (MUTF-8) encoding/decoding
/// per Android Dalvik DEX specification.
///
/// Differences between standard UTF-8 and MUTF-8:
/// 1. Null character U+0000 is encoded as two bytes: 0xC0 0x80 instead of 0x00.
/// 2. Code points > 0xFFFF (supplementary characters) are encoded as surrogate pairs,
///    where each 16-bit surrogate code unit is encoded as a 3-byte sequence (0xE0..).
///    Dalvik strictly forbids 4-byte UTF-8 sequences (0xF0..).
/// 3. String length in DEX `string_data_item` is the UTF-16 code unit count, not byte count.

pub fn encode_mutf8(s: &str) -> Vec<u8> {
    let mut bytes = Vec::with_capacity(s.len());
    for code_unit in s.encode_utf16() {
        if code_unit >= 0x0001 && code_unit <= 0x007F {
            bytes.push(code_unit as u8);
        } else if code_unit == 0 || (code_unit >= 0x0080 && code_unit <= 0x07FF) {
            bytes.push((0xC0 | ((code_unit >> 6) & 0x1F)) as u8);
            bytes.push((0x80 | (code_unit & 0x3F)) as u8);
        } else {
            // 0x0800..=0xFFFF (including UTF-16 surrogate code units 0xD800..=0xDFFF)
            bytes.push((0xE0 | ((code_unit >> 12) & 0x0F)) as u8);
            bytes.push((0x80 | ((code_unit >> 6) & 0x3F)) as u8);
            bytes.push((0x80 | (code_unit & 0x3F)) as u8);
        }
    }
    bytes
}

pub fn decode_mutf8(bytes: &[u8]) -> Result<String, String> {
    let mut utf16_units: Vec<u16> = Vec::new();
    let mut i = 0;
    while i < bytes.len() {
        let b = bytes[i];
        if (b & 0x80) == 0 {
            // 1-byte sequence
            utf16_units.push(b as u16);
            i += 1;
        } else if (b & 0xE0) == 0xC0 {
            // 2-byte sequence
            if i + 1 >= bytes.len() {
                return Err("Unexpected end of data in 2-byte MUTF-8 sequence".to_string());
            }
            let b2 = bytes[i + 1];
            let val = (((b & 0x1F) as u16) << 6) | ((b2 & 0x3F) as u16);
            utf16_units.push(val);
            i += 2;
        } else if (b & 0xF0) == 0xE0 {
            // 3-byte sequence
            if i + 2 >= bytes.len() {
                return Err("Unexpected end of data in 3-byte MUTF-8 sequence".to_string());
            }
            let b2 = bytes[i + 1];
            let b3 = bytes[i + 2];
            let val = (((b & 0x0F) as u16) << 12) | (((b2 & 0x3F) as u16) << 6) | ((b3 & 0x3F) as u16);
            utf16_units.push(val);
            i += 3;
        } else {
            return Err(format!("Invalid leading byte 0x{:02X} in MUTF-8 sequence", b));
        }
    }
    String::from_utf16(&utf16_units).map_err(|e| format!("Invalid UTF-16 surrogate pairs: {}", e))
}

pub fn utf16_length(s: &str) -> u32 {
    s.encode_utf16().count() as u32
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_ascii_roundtrip() {
        let text = "Hello, Dalvik DEX!";
        let encoded = encode_mutf8(text);
        assert_eq!(encoded, text.as_bytes());
        let decoded = decode_mutf8(&encoded).unwrap();
        assert_eq!(decoded, text);
    }

    #[test]
    fn test_null_character() {
        let text = "A\0B";
        let encoded = encode_mutf8(text);
        assert_eq!(encoded, vec![b'A', 0xC0, 0x80, b'B']);
        let decoded = decode_mutf8(&encoded).unwrap();
        assert_eq!(decoded, text);
    }

    #[test]
    fn test_supplementary_characters_surrogates() {
        // Rocket emoji U+1F680 (UTF-16: 0xD83D 0xDE80)
        let text = "🚀 KUI Android";
        let encoded = encode_mutf8(text);
        // Rocket should be two 3-byte sequences (0xED...)
        assert_eq!(encoded[0] & 0xF0, 0xE0);
        assert_eq!(encoded[3] & 0xF0, 0xE0);
        let decoded = decode_mutf8(&encoded).unwrap();
        assert_eq!(decoded, text);
        assert_eq!(utf16_length(text), 14); // rocket is 2 code units + 12 ASCII = 14
    }
}
