/// Dalvik Executable (DEX) file format constants and opcodes.

pub const DEX_FILE_MAGIC: &[u8; 8] = b"dex\n035\0";
pub const ENDIAN_CONSTANT: u32 = 0x12345678;
pub const HEADER_SIZE: u32 = 112;
pub const NO_INDEX: u32 = 0xFFFFFFFF;

// Map List item types
pub const TYPE_HEADER_ITEM: u16 = 0x0000;
pub const TYPE_STRING_ID_ITEM: u16 = 0x0001;
pub const TYPE_TYPE_ID_ITEM: u16 = 0x0002;
pub const TYPE_PROTO_ID_ITEM: u16 = 0x0003;
pub const TYPE_FIELD_ID_ITEM: u16 = 0x0004;
pub const TYPE_METHOD_ID_ITEM: u16 = 0x0005;
pub const TYPE_CLASS_DEF_ITEM: u16 = 0x0006;
pub const TYPE_MAP_LIST: u16 = 0x1000;
pub const TYPE_TYPE_LIST: u16 = 0x1001;
pub const TYPE_CLASS_DATA_ITEM: u16 = 0x2000;
pub const TYPE_CODE_ITEM: u16 = 0x2001;
pub const TYPE_STRING_DATA_ITEM: u16 = 0x2002;

// Dalvik Opcodes
pub const OP_NOP: u8 = 0x00;
pub const OP_RETURN_VOID: u8 = 0x0e;
pub const OP_RETURN: u8 = 0x0f;
pub const OP_RETURN_OBJECT: u8 = 0x11;
pub const OP_CONST_4: u8 = 0x12;
pub const OP_CONST_16: u8 = 0x13;
pub const OP_CONST_HIGH16: u8 = 0x15;
pub const OP_CONST_STRING: u8 = 0x1a;
pub const OP_NEW_INSTANCE: u8 = 0x22;
pub const OP_GOTO: u8 = 0x28;
pub const OP_IF_EQ: u8 = 0x32;
pub const OP_IF_NE: u8 = 0x33;
pub const OP_IGET_OBJECT: u8 = 0x54;
pub const OP_IPUT_OBJECT: u8 = 0x5b;
pub const OP_SGET_OBJECT: u8 = 0x62;
pub const OP_SPUT_OBJECT: u8 = 0x69;
pub const OP_INVOKE_VIRTUAL: u8 = 0x6e;
pub const OP_INVOKE_SUPER: u8 = 0x6f;
pub const OP_INVOKE_DIRECT: u8 = 0x70;
pub const OP_INVOKE_STATIC: u8 = 0x71;
pub const OP_INVOKE_INTERFACE: u8 = 0x72;
