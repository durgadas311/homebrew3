// Character-generator ROM for HDSP-2111 8-char 5x7 LED display device.

unsigned char fontTable[0x400] = {
	0x00,
	0b00010,
	0b00110,
	0b01110,
	0b11110,
	0b01110,
	0b00110,
	0b00010,
	0x01,
	0b00100,
	0b00000,
	0b00100,
	0b01000,
	0b10001,
	0b10001,
	0b01110,
	0x02,
	0b11111,
	0b00000,
	0b10001,
	0b01010,
	0b00100,
	0b01010,
	0b10001,
	0x03,
	0b11111,
	0b00000,
	0b10001,
	0b11001,
	0b10101,
	0b10011,
	0b10001,
	0x04,
	0b11111,
	0b00000,
	0b10110,
	0b11001,
	0b10001,
	0b10001,
	0b10001,
	0x05,
	0b00000,
	0b00000,
	0b01101,
	0b10010,
	0b10010,
	0b10010,
	0b01101,
	0x06,
	0b01100,
	0b10010,
	0b10010,
	0b10110,
	0b10001,
	0b10110,
	0b10000,
	0x07,
	0b00110,
	0b01000,
	0b00100,
	0b01110,
	0b10001,
	0b10001,
	0b01110,
	0x08,
	0b00000,
	0b00000,
	0b00000,
	0b00100,
	0b01010,
	0b10001,
	0b11111,
	0x09,
	0b00000,
	0b10000,
	0b11100,
	0b10010,
	0b10010,
	0b00010,
	0b00001,
	0x0a,
	0b01110,
	0b10001,
	0b10001,
	0b11111,
	0b10001,
	0b10001,
	0b01110,
	0x0b,
	0b00000,
	0b10000,
	0b01000,
	0b00100,
	0b01010,
	0b10001,
	0b10001,
	0x0c,
	0b00000,
	0b00000,
	0b01001,
	0b01001,
	0b01001,
	0b01110,
	0b10000,
	0x0d,
	0b00000,
	0b00001,
	0b01110,
	0b11010,
	0b01010,
	0b01010,
	0b01010,
	0x0e,
	0b00000,
	0b00000,
	0b01111,
	0b10010,
	0b10010,
	0b10010,
	0b01100,
	0x0f,
	0b11111,
	0b01000,
	0b00100,
	0b00010,
	0b00100,
	0b01000,
	0b11111,
	0x10,
	0b00000,
	0b00000,
	0b00001,
	0b01110,
	0b10100,
	0b00100,
	0b00100,
	0x11,
	0b00000,
	0b00100,
	0b01110,
	0b10101,
	0b10101,
	0b01110,
	0b00100,
	0x12,
	0b01110,
	0b10001,
	0b10001,
	0b10001,
	0b10001,
	0b01010,
	0b11011,
	0x13,
	0b00100,
	0b00000,
	0b01110,
	0b10001,
	0b11111,
	0b10001,
	0b10001,
	0x14,
	0b00100,
	0b00000,
	0b01110,
	0b10010,
	0b10010,
	0b10010,
	0b01101,
	0x15,
	0b01010,
	0b00000,
	0b01110,
	0b10001,
	0b11111,
	0b10001,
	0b10001,
	0x16,
	0b01010,
	0b00000,
	0b01110,
	0b10010,
	0b10010,
	0b10010,
	0b01101,
	0x17,
	0b01010,
	0b01110,
	0b10001,
	0b10001,
	0b10001,
	0b10001,
	0b01110,
	0x18,
	0b01010,
	0b00000,
	0b01110,
	0b10001,
	0b10001,
	0b10001,
	0b01110,
	0x19,
	0b01010,
	0b00000,
	0b10001,
	0b10001,
	0b10001,
	0b10001,
	0b01110,
	0x1a,
	0b00000,
	0b01010,
	0b00000,
	0b10001,
	0b10001,
	0b10001,
	0b01110,
	0x1b,
	0b00000,
	0b00100,
	0b00010,
	0b11111,
	0b00010,
	0b00100,
	0b00000,
	0x1c,
	0b00000,
	0b01111,
	0b01000,
	0b01000,
	0b01000,
	0b11000,
	0b01000,
	0x1d,
	0b01100,
	0b10010,
	0b00100,
	0b01000,
	0b11110,
	0b00000,
	0b00000,
	0x1e,
	0b00110,
	0b01001,
	0b01000,
	0b11100,
	0b01000,
	0b01000,
	0b11111,
	0x1f,
	0b10001,
	0b01010,
	0b00100,
	0b00100,
	0b01110,
	0b00100,
	0b00100,
	0x20,	// Char 20 ' '
	0b00000,
	0b00000,
	0b00000,
	0b00000,
	0b00000,
	0b00000,
	0b00000,
	0x21,	// Char 21 '!'
	0b01000,
	0b01000,
	0b01000,
	0b01000,
	0b01000,
	0b00000,
	0b01000,
	0x22,	// Char 22 '"'
	0b01010,
	0b01010,
	0b00000,
	0b00000,
	0b00000,
	0b00000,
	0b00000,
	0x23,	// Char 23 '#'
	0b01010,
	0b01010,
	0b11111,
	0b01010,
	0b11111,
	0b01010,
	0b01010,
	0x24,	// Char 24 '$'
	0b00100,
	0b01111,
	0b10000,
	0b01110,
	0b00001,
	0b11110,
	0b00100,
	0x25,	// Char 25 '%'
	0b11000,
	0b11001,
	0b00010,
	0b00100,
	0b01000,
	0b10011,
	0b00011,
	0x26,	// Char 26 '&'
	0b01000,
	0b10100,
	0b10100,
	0b01000,
	0b10101,
	0b10010,
	0b01101,
	0x27,	// Char 27 '''
	0b01100,
	0b01100,
	0b00100,
	0b01000,
	0b00000,
	0b00000,
	0b00000,
	0x28,	// Char 28 '('
	0b00010,
	0b00100,
	0b00100,
	0b00100,
	0b00100,
	0b00100,
	0b00010,
	0x29,	// Char 29 ')'
	0b01000,
	0b00100,
	0b00100,
	0b00100,
	0b00100,
	0b00100,
	0b01000,
	0x2a,	// Char 2a '*'
	0b00000,
	0b01010,
	0b00100,
	0b11111,
	0b00100,
	0b01010,
	0b00000,
	0x2b,	// Char 2b '+'
	0b00000,
	0b00100,
	0b00100,
	0b11111,
	0b00100,
	0b00100,
	0b00000,
	0x2c,	// Char 2c ','
	0b00000,
	0b00000,
	0b00000,
	0b01100,
	0b01100,
	0b00100,
	0b01000,
	0x2d,	// Char 2d '-'
	0b00000,
	0b00000,
	0b00000,
	0b11111,
	0b00000,
	0b00000,
	0b00000,
	0x2e,	// Char 2e '.'
	0b00000,
	0b00000,
	0b00000,
	0b00000,
	0b00000,
	0b01100,
	0b01100,
	0x2f,	// Char 2f '/'
	0b00000,
	0b00001,
	0b00010,
	0b00100,
	0b01000,
	0b10000,
	0b00000,
	0x30,	// Char 30 '0'
	0b01110,
	0b10001,
	0b10011,
	0b10101,
	0b11001,
	0b10001,
	0b01110,
	0x31,	// Char 31 '1'
	0b00100,
	0b01100,
	0b00100,
	0b00100,
	0b00100,
	0b00100,
	0b01110,
	0x32,	// Char 32 '2'
	0b01110,
	0b10001,
	0b00001,
	0b00110,
	0b01000,
	0b10000,
	0b11111,
	0x33,	// Char 33 '3'
	0b01110,
	0b10001,
	0b00001,
	0b00110,
	0b00001,
	0b10001,
	0b01110,
	0x34,	// Char 34 '4'
	0b00010,
	0b00110,
	0b01010,
	0b10010,
	0b11111,
	0b00010,
	0b00010,
	0x35,	// Char 35 '5'
	0b11111,
	0b10000,
	0b11110,
	0b00001,
	0b00001,
	0b10001,
	0b01110,
	0x36,	// Char 36 '6'
	0b00110,
	0b01000,
	0b10000,
	0b11110,
	0b10001,
	0b10001,
	0b01110,
	0x37,	// Char 37 '7'
	0b11111,
	0b00001,
	0b00010,
	0b00100,
	0b01000,
	0b01000,
	0b01000,
	0x38,	// Char 38 '8'
	0b01110,
	0b10001,
	0b10001,
	0b01110,
	0b10001,
	0b10001,
	0b01110,
	0x39,	// Char 39 '9'
	0b01110,
	0b10001,
	0b10001,
	0b01111,
	0b00001,
	0b00010,
	0b01100,
	0x3a,	// Char 3a ':'
	0b00000,
	0b01100,
	0b01100,
	0b00000,
	0b01100,
	0b01100,
	0b00000,
	0x3b,	// Char 3b ';'
	0b01100,
	0b01100,
	0b00000,
	0b01100,
	0b01100,
	0b00100,
	0b01000,
	0x3c,	// Char 3c '<'
	0b00001,
	0b00010,
	0b00100,
	0b01000,
	0b00100,
	0b00010,
	0b00001,
	0x3d,	// Char 3d '='
	0b00000,
	0b00000,
	0b11111,
	0b00000,
	0b11111,
	0b00000,
	0b00000,
	0x3e,	// Char 3e '>'
	0b10000,
	0b01000,
	0b00100,
	0b00010,
	0b00100,
	0b01000,
	0b10000,
	0x3f,	// Char 3f '?'
	0b01110,
	0b10001,
	0b00001,
	0b00010,
	0b00100,
	0b00000,
	0b00100,
	0x40,	// Char 40 '@'
	0b01110,
	0b10001,
	0b10111,
	0b10101,
	0b10111,
	0b10000,
	0b01110,
	0x41,	// Char 41 'A'
	0b01110,
	0b10001,
	0b10001,
	0b11111,
	0b10001,
	0b10001,
	0b10001,
	0x42,	// Char 42 'B'
	0b11110,
	0b10001,
	0b10001,
	0b11110,
	0b10001,
	0b10001,
	0b11110,
	0x43,	// Char 43 'C'
	0b01110,
	0b10001,
	0b10000,
	0b10000,
	0b10000,
	0b10001,
	0b01110,
	0x44,	// Char 44 'D'
	0b11110,
	0b10001,
	0b10001,
	0b10001,
	0b10001,
	0b10001,
	0b11110,
	0x45,	// Char 45 'E'
	0b11111,
	0b10000,
	0b10000,
	0b11110,
	0b10000,
	0b10000,
	0b11111,
	0x46,	// Char 46 'F'
	0b11111,
	0b10000,
	0b10000,
	0b11110,
	0b10000,
	0b10000,
	0b10000,
	0x47,	// Char 47 'G'
	0b01110,
	0b10001,
	0b10000,
	0b10000,
	0b10011,
	0b10001,
	0b01110,
	0x48,	// Char 48 'H'
	0b10001,
	0b10001,
	0b10001,
	0b11111,
	0b10001,
	0b10001,
	0b10001,
	0x49,	// Char 49 'I'
	0b01110,
	0b00100,
	0b00100,
	0b00100,
	0b00100,
	0b00100,
	0b01110,
	0x4a,	// Char 4a 'J'
	0b00001,
	0b00001,
	0b00001,
	0b00001,
	0b00001,
	0b10001,
	0b01110,
	0x4b,	// Char 4b 'K'
	0b10001,
	0b10010,
	0b10100,
	0b11000,
	0b10100,
	0b10010,
	0b10001,
	0x4c,	// Char 4c 'L'
	0b10000,
	0b10000,
	0b10000,
	0b10000,
	0b10000,
	0b10000,
	0b11111,
	0x4d,	// Char 4d 'M'
	0b10001,
	0b11011,
	0b10101,
	0b10101,
	0b10001,
	0b10001,
	0b10001,
	0x4e,	// Char 4e 'N'
	0b10001,
	0b10001,
	0b11001,
	0b10101,
	0b10011,
	0b10001,
	0b10001,
	0x4f,	// Char 4f 'O'
	0b01110,
	0b10001,
	0b10001,
	0b10001,
	0b10001,
	0b10001,
	0b01110,
	0x50,	// Char 50 'P'
	0b11110,
	0b10001,
	0b10001,
	0b11110,
	0b10000,
	0b10000,
	0b10000,
	0x51,	// Char 51 'Q'
	0b01110,
	0b10001,
	0b10001,
	0b10001,
	0b10101,
	0b10010,
	0b01101,
	0x52,	// Char 52 'R'
	0b11110,
	0b10001,
	0b10001,
	0b11110,
	0b10100,
	0b10010,
	0b10001,
	0x53,	// Char 53 'S'
	0b01110,
	0b10001,
	0b10000,
	0b01110,
	0b00001,
	0b10001,
	0b01110,
	0x54,	// Char 54 'T'
	0b11111,
	0b00100,
	0b00100,
	0b00100,
	0b00100,
	0b00100,
	0b00100,
	0x55,	// Char 55 'U'
	0b10001,
	0b10001,
	0b10001,
	0b10001,
	0b10001,
	0b10001,
	0b01110,
	0x56,	// Char 56 'V'
	0b10001,
	0b10001,
	0b10001,
	0b01010,
	0b01010,
	0b00100,
	0b00100,
	0x57,	// Char 57 'W'
	0b10001,
	0b10001,
	0b10001,
	0b10101,
	0b10101,
	0b11011,
	0b10001,
	0x58,	// Char 58 'X'
	0b10001,
	0b10001,
	0b01010,
	0b00100,
	0b01010,
	0b10001,
	0b10001,
	0x59,	// Char 59 'Y'
	0b10001,
	0b10001,
	0b01010,
	0b00100,
	0b00100,
	0b00100,
	0b00100,
	0x5a,	// Char 5a 'Z'
	0b11111,
	0b00001,
	0b00010,
	0b00100,
	0b01000,
	0b10000,
	0b11111,
	0x5b,	// Char 5b '['
	0b00111,
	0b00100,
	0b00100,
	0b00100,
	0b00100,
	0b00100,
	0b00111,
	0x5c,	// Char 5c '\'
	0b00000,
	0b10000,
	0b01000,
	0b00100,
	0b00010,
	0b00001,
	0b00000,
	0x5d,	// Char 5d ']'
	0b11100,
	0b00100,
	0b00100,
	0b00100,
	0b00100,
	0b00100,
	0b11100,
	0x5e,	// Char 5e '^'
	0b00100,
	0b01110,
	0b10101,
	0b00100,
	0b00100,
	0b00100,
	0b00100,
	0x5f,	// Char 5f '_'
	0b00000,
	0b00000,
	0b00000,
	0b00000,
	0b00000,
	0b00000,
	0b11111,
	0x60,	// Char 60 '@'
	0b01100,
	0b01100,
	0b01000,
	0b00100,
	0b00000,
	0b00000,
	0b00000,
	0x61,	// Char 60 'A'
	0b00000,
	0b00000,
	0b01110,
	0b10010,
	0b10010,
	0b10010,
	0b01101,
	0x62,	// Char 62 'B'
	0b10000,
	0b10000,
	0b10110,
	0b11001,
	0b10001,
	0b10001,
	0b11110,
	0x63,	// Char 63 'C'
	0b00000,
	0b00000,
	0b01110,
	0b10000,
	0b10000,
	0b10001,
	0b01110,
	0x64,	// Char 64 'D'
	0b00001,
	0b00001,
	0b01101,
	0b10011,
	0b10001,
	0b10001,
	0b01111,
	0x65,	// Char 65 'E'
	0b00000,
	0b00000,
	0b01110,
	0b10001,
	0b11110,
	0b10000,
	0b01110,
	0x66,	// Char 66 'F'
	0b00100,
	0b01010,
	0b01000,
	0b11100,
	0b01000,
	0b01000,
	0b01000,
	0x67,	// Char 67 'G'
	0b00000,
	0b00000,
	0b01111,
	0b10001,
	0b01111,
	0b00001,
	0b00110,
	0x68,	// Char 68 'H'
	0b10000,
	0b10000,
	0b10110,
	0b11001,
	0b10001,
	0b10001,
	0b10001,
	0x69,	// Char 69 'I'
	0b00100,
	0b00000,
	0b01100,
	0b00100,
	0b00100,
	0b00100,
	0b01110,
	0x6a,	// Char 6a 'J'
	0b00010,
	0b00000,
	0b00110,
	0b00010,
	0b00010,
	0b10010,
	0b01100,
	0x6b,	// Char 6b 'K'
	0b01000,
	0b01000,
	0b01001,
	0b01010,
	0b01100,
	0b01010,
	0b01001,
	0x6c,	// Char 6c 'L'
	0b01100,
	0b00100,
	0b00100,
	0b00100,
	0b00100,
	0b00100,
	0b01110,
	0x6d,	// Char 6d 'M'
	0b00000,
	0b00000,
	0b01010,
	0b10101,
	0b10101,
	0b10001,
	0b10001,
	0x6e,	// Char 6e 'N'
	0b00000,
	0b00000,
	0b10110,
	0b11001,
	0b10001,
	0b10001,
	0b10001,
	0x6f,	// Char 6f 'O'
	0b00000,
	0b00000,
	0b01110,
	0b10001,
	0b10001,
	0b10001,
	0b01110,
	0x70,	// Char 70 'P'
	0b00000,
	0b00000,
	0b11110,
	0b10001,
	0b11001,
	0b10110,
	0b10000,
	0x71,	// Char 70 'Q'
	0b00000,
	0b00000,
	0b01110,
	0b10010,
	0b10110,
	0b01010,
	0b00011,
	0x72,	// Char 72 'R'
	0b00000,
	0b00000,
	0b01011,
	0b01100,
	0b01000,
	0b01000,
	0b01000,
	0x73,	// Char 73 'S'
	0b00000,
	0b00000,
	0b01110,
	0b10000,
	0b01110,
	0b00001,
	0b11110,
	0x74,	// Char 74 'T'
	0b00000,
	0b01000,
	0b11100,
	0b01000,
	0b01000,
	0b01010,
	0b00100,
	0x75,	// Char 75 'U'
	0b00000,
	0b00000,
	0b10001,
	0b10001,
	0b10001,
	0b10011,
	0b01101,
	0x76,	// Char 76 'V'
	0b00000,
	0b00000,
	0b10001,
	0b10001,
	0b10001,
	0b01010,
	0b00100,
	0x77,	// Char 77 'W'
	0b00000,
	0b00000,
	0b10001,
	0b10001,
	0b10101,
	0b10101,
	0b01010,
	0x78,	// Char 78 'X'
	0b00000,
	0b00000,
	0b10001,
	0b01010,
	0b00100,
	0b01010,
	0b10001,
	0x79,	// Char 79 'Y'
	0b00000,
	0b00000,
	0b10001,
	0b01010,
	0b00100,
	0b00100,
	0b01000,
	0x7a,	// Char 7a 'Z'
	0b00000,
	0b00000,
	0b11111,
	0b00010,
	0b00100,
	0b01000,
	0b11111,
	0x7b,	// Char 7b '['
	0b00010,
	0b00100,
	0b00100,
	0b01000,
	0b00100,
	0b00100,
	0b00010,
	0x7c,	// Char 7c '\'
	0b00100,
	0b00100,
	0b00100,
	0b00000,
	0b00100,
	0b00100,
	0b00100,
	0x7d,	// Char 7d ']'
	0b01000,
	0b00100,
	0b00100,
	0b00010,
	0b00100,
	0b00100,
	0b01000,
	0x7e,	// Char 7e '^'
	0b00000,
	0b00000,
	0b01000,
	0b10101,
	0b00010,
	0b00000,
	0b00000,
	0x7f,	// Char 7f '_'
	0b01010,
	0b10101,
	0b01010,
	0b10101,
	0b01010,
	0b10101,
	0b01010,
};