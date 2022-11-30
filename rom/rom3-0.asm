; EPROM fault on A8, lost all odd pages.
; Here, all odd pages have been filled with 0xc9 before disassembly.
;
	maclib	z80

GONE	equ	0c9h	; contents missing of ROM sections

CR	equ	13
LF	equ	10

; Z80A-CTC ports
ctc0	equ	00h
ctc1	equ	01h
ctc2	equ	02h
ctc3	equ	03h

; Z80A-SIO ports
sioAdat	equ	04h
sioBdat	equ	05h
sioActl	equ	06h
sioBctl	equ	07h

; Z80A-PIO #1 ports (U14)
pio1Ad	equ	08h
pio1Bd	equ	09h
pio1Ac	equ	0ah
pio1Bc	equ	0bh
; Z80A-PIO #2 ports (U15)
pio2Ad	equ	0ch
pio2Bd	equ	0dh
pio2Ac	equ	0eh
pio2Bc	equ	0fh

	org	00000h
L0000:	mvi	a,0c9h		;; 0000: 3e c9       >.
	sta	L1fdc		;; 0002: 32 dc 1f    2..
	jr	L0071		;; 0005: 18 6a       .j
	db	0ffh

; debug trap/breakpoint
RST1:	xthl			;; 0008: e3          .
	dcx	h		;; 0009: 2b          +
	xthl			;; 000a: e3          .
	jr	L0066		;; 000b: 18 59       .Y
	db	0ffh,0ffh,0ffh

RST2:	db	0ffh,0ffh,0ffh,0ffh,0ffh,0ffh,0ffh,0ffh

RST3:	db	0ffh,0ffh,0ffh,0ffh,0ffh,0ffh,0ffh,0ffh

RST4:	db	0ffh,0ffh,0ffh,0ffh,0ffh,0ffh,0ffh,0ffh

RST5:	db	0ffh,0ffh,0ffh,0ffh,0ffh,0ffh,0ffh,0ffh

RST6:	db	0ffh,0ffh,0ffh,0ffh,0ffh,0ffh,0ffh,0ffh

RST7:	jmp	L1fde		; debug entry?
	db	0ffh,0ffh,0ffh,0ffh,0ffh

; program utility routines
L0040:	jmp	L0094		;; 0040: c3 94 00    ...
L0043:	jmp	L01ce		; console input?
L0046:	jmp	L01e1		; console output (A)
L0049:	jmp	L01c4		;; 0049: c3 c4 01    ...
L004c:	jmp	L03df		;; 004c: c3 df 03    ...

	db	0ffh,0ffh,0ffh,0ffh,0ffh,0ffh,0ffh,0ffh,0ffh,0ffh,0ffh,0ffh
	db	0ffh,0ffh,0ffh,0ffh,0ffh,0ffh,0ffh,0ffh,0ffh,0ffh,0ffh

; NMI - pushbutton - manual program break.
L0066:	push	psw		;; 0066: f5          .
	lda	L1ffc		;; 0067: 3a fc 1f    :..
	ana	a		;; 006a: a7          .
	jrnz	L0077		;; 006b: 20 0a        .
	pop	psw		;; 006d: f1          .
	jmp	L02a3		;; 006e: c3 a3 02    ...

L0071:	lxi	h,L1fa0		;; 0071: 21 a0 1f    ...
	shld	L1fd6		;; 0074: 22 d6 1f    "..
L0077:	lxi	sp,L1fc0	;; 0077: 31 c0 1f    1..
	lxi	h,L03b2		;; 007a: 21 b2 03    ...
	mvi	b,20		;; 007d: 06 14       ..
	call	L020e		;; 007f: cd 0e 02    ...
	mvi	a,010h		;; SIO Reset Ext/Status Intrs
	out	sioActl		;; 0084: d3 06       ..
	in	sioActl		;; 0086: db 06       ..
	bit	5,a		;; test CTS - 4800/9600
	jrz	L0094		;; 008a: 28 08       (.
	lxi	h,L03da		;; Program alternate baud
	mvi	b,2		;; 008f: 06 02       ..
	call	L020e		;; 0091: cd 0e 02    ...
L0094:	call	L01bc		;; 0094: cd bc 01    ...
	lxi	b,1		;; 0097: 01 01 00    ...
	sbcd	L1ffc		;; 009a: ed 43 fc 1f .C..
	dcr	c		;; 009e: 0d          .
	xra	a		;; 009f: af          .
	sta	L1ffb		;; 00a0: 32 fb 1f    2..
	call	L01a6		;; 00a3: cd a6 01    ...
	jrc	L00bf		;; 00a6: 38 17       8.
	lxi	h,L0000		;; 00a8: 21 00 00    ...
	jr	L00b2		;; 00ab: 18 05       ..

; wait for input (from SIO? keypad? ???)
L00ad:	call	L01a6		;; 00ad: cd a6 01    ...
	jrc	L00bf		;; 00b0: 38 0d       8.
L00b2:	inr	a		;; 00b2: 3c          <
	sta	L1ffb		;; 00b3: 32 fb 1f    2..
	dcr	a		;; 00b6: 3d          =
	dad	h		;; 00b7: 29          )
	dad	h		;; 00b8: 29          )
	dad	h		;; 00b9: 29          )
	dad	h		;; 00ba: 29          )
	add	l		;; 00bb: 85          .
	mov	l,a		;; 00bc: 6f          o
	jr	L00ad		;; 00bd: 18 ee       ..

; got input, parse command char
L00bf:	cpi	'G'		;; 00bf: fe 47       .G
	jrz	L00fc		;; 00c1: 28 39       (9
	cpi	'/'		;; 00c3: fe 2f       ./
	jrz	L011f		;; 00c5: 28 58       (X
	cpi	'V'		;; 00c7: fe 56       .V
	jrz	L012d		;; 00c9: 28 62       (b
	cpi	'I'		;; 00cb: fe 49       .I
	jz	L0151		;; 00cd: ca 51 01    .Q.
	cpi	'T'		;; 00d0: fe 54       .T
	jrz	L0127		;; 00d2: 28 53       (S
	cpi	'P'		;; 00d4: fe 50       .P
	jz	L00f6		;; 00d6: ca f6 00    ...
	cpi	'R'		;; 00d9: fe 52       .R
	jz	L026b		;; 00db: ca 6b 02    .k.
	cpi	'H'		;; 00de: fe 48       .H
	jz	L0480		;; 00e0: ca 80 04    ...
	cpi	LF		;; 00e3: fe 0a       ..
	jrz	L0109		;; 00e5: 28 22       ("
	cpi	CR		;; 00e7: fe 0d       ..
	jrz	L0112		;; 00e9: 28 27       ('
	cpi	'^'		;; 00eb: fe 5e       .^
	jrz	L0118		;; 00ed: 28 29       ()
; error re-entry to monitor loop...
L00ef:	mvi	a,'?'		;; 00ef: 3e 3f       >?
	call	L01e1		;; 00f1: cd e1 01    ...
	jr	L0094		;; 00f4: 18 9e       ..

L00f6:	lhld	L1fd8		;; 00f6: 2a d8 1f    *..
	inx	h		;; 00f9: 23          #
	jr	L0103		;; 00fa: 18 07       ..

L00fc:	lda	L1ffb		;; 00fc: 3a fb 1f    :..
	ana	a		;; 00ff: a7          .
	ret			;; 0100: c9          .
	ret			;; 0101: c9          .
	ret			;; 0102: c9          .
L0103:	ret			;; 0103: c9          .
	ret			;; 0104: c9          .
	ret			;; 0105: c9          .
	ret			;; 0106: c9          .
	ret			;; 0107: c9          .
	ret			;; 0108: c9          .
L0109:	ret			;; 0109: c9          .
	ret			;; 010a: c9          .
	ret			;; 010b: c9          .
	ret			;; 010c: c9          .
	ret			;; 010d: c9          .
	ret			;; 010e: c9          .
	ret			;; 010f: c9          .
	ret			;; 0110: c9          .
	ret			;; 0111: c9          .
L0112:	ret			;; 0112: c9          .
	ret			;; 0113: c9          .
	ret			;; 0114: c9          .
	ret			;; 0115: c9          .
	ret			;; 0116: c9          .
	ret			;; 0117: c9          .
L0118:	ret			;; 0118: c9          .
	ret			;; 0119: c9          .
	ret			;; 011a: c9          .
	ret			;; 011b: c9          .
	ret			;; 011c: c9          .
	ret			;; 011d: c9          .
	ret			;; 011e: c9          .
L011f:	ret			;; 011f: c9          .
	ret			;; 0120: c9          .
	ret			;; 0121: c9          .
	ret			;; 0122: c9          .
	ret			;; 0123: c9          .
	ret			;; 0124: c9          .
	ret			;; 0125: c9          .
	ret			;; 0126: c9          .
L0127:	ret			;; 0127: c9          .
	ret			;; 0128: c9          .
	ret			;; 0129: c9          .
	ret			;; 012a: c9          .
	ret			;; 012b: c9          .
	ret			;; 012c: c9          .
L012d:	ret			;; 012d: c9          .
	ret			;; 012e: c9          .
	ret			;; 012f: c9          .
	ret			;; 0130: c9          .
	ret			;; 0131: c9          .
	ret			;; 0132: c9          .
	ret			;; 0133: c9          .
	ret			;; 0134: c9          .
	ret			;; 0135: c9          .
	ret			;; 0136: c9          .
	ret			;; 0137: c9          .
	ret			;; 0138: c9          .
	ret			;; 0139: c9          .
	ret			;; 013a: c9          .
	ret			;; 013b: c9          .
	ret			;; 013c: c9          .
	ret			;; 013d: c9          .
	ret			;; 013e: c9          .
	ret			;; 013f: c9          .
	ret			;; 0140: c9          .
	ret			;; 0141: c9          .
	ret			;; 0142: c9          .
	ret			;; 0143: c9          .
	ret			;; 0144: c9          .
	ret			;; 0145: c9          .
	ret			;; 0146: c9          .
	ret			;; 0147: c9          .
	ret			;; 0148: c9          .
	ret			;; 0149: c9          .
	ret			;; 014a: c9          .
	ret			;; 014b: c9          .
	ret			;; 014c: c9          .
	ret			;; 014d: c9          .
	ret			;; 014e: c9          .
	ret			;; 014f: c9          .
	ret			;; 0150: c9          .
L0151:	ret			;; 0151: c9          .
	ret			;; 0152: c9          .
	ret			;; 0153: c9          .
	ret			;; 0154: c9          .
	ret			;; 0155: c9          .
	ret			;; 0156: c9          .
	ret			;; 0157: c9          .
	ret			;; 0158: c9          .
	ret			;; 0159: c9          .
	ret			;; 015a: c9          .
	ret			;; 015b: c9          .
	ret			;; 015c: c9          .
	ret			;; 015d: c9          .
	ret			;; 015e: c9          .
	ret			;; 015f: c9          .
	ret			;; 0160: c9          .
	ret			;; 0161: c9          .
	ret			;; 0162: c9          .
	ret			;; 0163: c9          .
	ret			;; 0164: c9          .
	ret			;; 0165: c9          .
	ret			;; 0166: c9          .
	ret			;; 0167: c9          .
	ret			;; 0168: c9          .
	ret			;; 0169: c9          .
	ret			;; 016a: c9          .
	ret			;; 016b: c9          .
	ret			;; 016c: c9          .
	ret			;; 016d: c9          .
	ret			;; 016e: c9          .
	ret			;; 016f: c9          .
	ret			;; 0170: c9          .
	ret			;; 0171: c9          .
	ret			;; 0172: c9          .
	ret			;; 0173: c9          .
	ret			;; 0174: c9          .
	ret			;; 0175: c9          .
	ret			;; 0176: c9          .
	ret			;; 0177: c9          .
	ret			;; 0178: c9          .
	ret			;; 0179: c9          .
	ret			;; 017a: c9          .
	ret			;; 017b: c9          .
	ret			;; 017c: c9          .
	ret			;; 017d: c9          .
	ret			;; 017e: c9          .
	ret			;; 017f: c9          .
	ret			;; 0180: c9          .
	ret			;; 0181: c9          .
	ret			;; 0182: c9          .
	ret			;; 0183: c9          .
	ret			;; 0184: c9          .
	ret			;; 0185: c9          .
	ret			;; 0186: c9          .
	ret			;; 0187: c9          .
	ret			;; 0188: c9          .
	ret			;; 0189: c9          .
	ret			;; 018a: c9          .
	ret			;; 018b: c9          .
	ret			;; 018c: c9          .
	ret			;; 018d: c9          .
	ret			;; 018e: c9          .
	ret			;; 018f: c9          .
	ret			;; 0190: c9          .
	ret			;; 0191: c9          .
	ret			;; 0192: c9          .
	ret			;; 0193: c9          .
	ret			;; 0194: c9          .
	ret			;; 0195: c9          .
	ret			;; 0196: c9          .
	ret			;; 0197: c9          .
	ret			;; 0198: c9          .
	ret			;; 0199: c9          .
	ret			;; 019a: c9          .
	ret			;; 019b: c9          .
	ret			;; 019c: c9          .
	ret			;; 019d: c9          .
	ret			;; 019e: c9          .
	ret			;; 019f: c9          .
	ret			;; 01a0: c9          .
	ret			;; 01a1: c9          .
	ret			;; 01a2: c9          .
	ret			;; 01a3: c9          .
	ret			;; 01a4: c9          .
	ret			;; 01a5: c9          .
L01a6:	ret			;; 01a6: c9          .
	ret			;; 01a7: c9          .
	ret			;; 01a8: c9          .
	ret			;; 01a9: c9          .
	ret			;; 01aa: c9          .
	ret			;; 01ab: c9          .
	ret			;; 01ac: c9          .
	ret			;; 01ad: c9          .
	ret			;; 01ae: c9          .
	ret			;; 01af: c9          .
	ret			;; 01b0: c9          .
	ret			;; 01b1: c9          .
	ret			;; 01b2: c9          .
	ret			;; 01b3: c9          .
	ret			;; 01b4: c9          .
	ret			;; 01b5: c9          .
	ret			;; 01b6: c9          .
	ret			;; 01b7: c9          .
	ret			;; 01b8: c9          .
	ret			;; 01b9: c9          .
	ret			;; 01ba: c9          .
	ret			;; 01bb: c9          .
L01bc:	ret			;; 01bc: c9          .
	ret			;; 01bd: c9          .
	ret			;; 01be: c9          .
	ret			;; 01bf: c9          .
	ret			;; 01c0: c9          .
	ret			;; 01c1: c9          .
	ret			;; 01c2: c9          .
	ret			;; 01c3: c9          .

L01c4:	ret			;; 01c4: c9          .
	ret			;; 01c5: c9          .
	ret			;; 01c6: c9          .
	ret			;; 01c7: c9          .
	ret			;; 01c8: c9          .
	ret			;; 01c9: c9          .
	ret			;; 01ca: c9          .
	ret			;; 01cb: c9          .
	ret			;; 01cc: c9          .
	ret			;; 01cd: c9          .

; console input?
L01ce:	ret			;; 01ce: c9          .
	ret			;; 01cf: c9          .
	ret			;; 01d0: c9          .
	ret			;; 01d1: c9          .
	ret			;; 01d2: c9          .
	ret			;; 01d3: c9          .
	ret			;; 01d4: c9          .
	ret			;; 01d5: c9          .
	ret			;; 01d6: c9          .
	ret			;; 01d7: c9          .
	ret			;; 01d8: c9          .
	ret			;; 01d9: c9          .
	ret			;; 01da: c9          .
	ret			;; 01db: c9          .
	ret			;; 01dc: c9          .
	ret			;; 01dd: c9          .
	ret			;; 01de: c9          .
	ret			;; 01df: c9          .
	ret			;; 01e0: c9          .

; console output from A
L01e1:	ret			;; 01e1: c9          .
	ret			;; 01e2: c9          .
	ret			;; 01e3: c9          .
	ret			;; 01e4: c9          .
	ret			;; 01e5: c9          .
	ret			;; 01e6: c9          .
	ret			;; 01e7: c9          .
	ret			;; 01e8: c9          .
	ret			;; 01e9: c9          .
	ret			;; 01ea: c9          .
	ret			;; 01eb: c9          .
	ret			;; 01ec: c9          .
	ret			;; 01ed: c9          .
	ret			;; 01ee: c9          .
	ret			;; 01ef: c9          .
	ret			;; 01f0: c9          .
	ret			;; 01f1: c9          .
	ret			;; 01f2: c9          .
	ret			;; 01f3: c9          .
	ret			;; 01f4: c9          .
	ret			;; 01f5: c9          .
	ret			;; 01f6: c9          .
	ret			;; 01f7: c9          .
	ret			;; 01f8: c9          .
	ret			;; 01f9: c9          .
	ret			;; 01fa: c9          .
	ret			;; 01fb: c9          .
	ret			;; 01fc: c9          .
	ret			;; 01fd: c9          .
	ret			;; 01fe: c9          .
	ret			;; 01ff: c9          .
; ... convert ASCII to HEX
	cmc			;; 0200: 3f          ?
	rc			;; 0201: d8          .
	cpi	'9'+1		;; 0202: fe 3a       .:
	jrc	L020b		;; 0204: 38 05       8.
	cpi	'A'		;; 0206: fe 41       .A
	rc			;; 0208: d8          .
	sui	007h		;; 0209: d6 07       ..
L020b:	sui	'0'		;; 020b: d6 30       .0
	ret			;; 020d: c9          .

; I/O init: process 'B' pairs of port/data at HL
L020e:	mov	c,m		;; 020e: 4e          N
	inx	h		;; 020f: 23          #
	outi			;; 0210: ed a3       ..
	jrnz	L020e		;; 0212: 20 fa        .
	ret			;; 0214: c9          .

L0215:	push	b		;; 0215: c5          .
	push	h		;; 0216: e5          .
	lxi	b,-L1fda	;; 0217: 01 26 e0    .&.
	dad	b		;; 021a: 09          .
	jrc	L0223		;; 021b: 38 06       8.
	lxi	b,01ah		;; 021d: 01 1a 00    ...
	dad	b		;; 0220: 09          .
	jrc	L0247		;; 0221: 38 24       8$
; output HL in HEX
L0223:	pop	h		;; 0223: e1          .
	pop	b		;; 0224: c1          .
	mov	a,h		;; 0225: 7c          |
	call	L022a		;; 0226: cd 2a 02    .*.
	mov	a,l		;; 0229: 7d          }
; output A in HEX
L022a:	push	psw		;; 022a: f5          .
	rrcr	a		;; 022b: cb 0f       ..
	rrcr	a		;; 022d: cb 0f       ..
	rrcr	a		;; 022f: cb 0f       ..
	rrcr	a		;; 0231: cb 0f       ..
	call	L0237		;; 0233: cd 37 02    .7.
	pop	psw		;; 0236: f1          .
L0237:	call	L023d		;; 0237: cd 3d 02    .=.
	jmp	L01e1		;; 023a: c3 e1 01    ...

; HEX to ASCII
L023d:	ani	00fh		;; 023d: e6 0f       ..
	adi	'0'		;; 023f: c6 30       .0
	cpi	'9'+1		;; 0241: fe 3a       .:
	rm			;; 0243: f8          .
	adi	007h		;; 0244: c6 07       ..
	ret			;; 0246: c9          .

; output char at HL, annotations
L0247:	pop	h		;; 0247: e1          .
	push	h		;; 0248: e5          .
	lxi	b,-L1c27	;; 0249: 01 d9 e3    ...
	dad	b		;; 024c: 09          .
	mvi	a,'R'		;; 024d: 3e 52       >R
	call	L01e1		;; 024f: cd e1 01    ...
	mov	a,m		;; 0252: 7e          ~
	ana	a		;; 0253: a7          .
	jrnz	L025a		;; 0254: 20 04        .
	mvi	a,'+'		;; 0256: 3e 2b       >+
	jr	L0265		;; 0258: 18 0b       ..

; output char but annotate lowercase letters
L025a:	bit	5,a		;; 025a: cb 6f       .o
	jrz	L0265		;; 025c: 28 07       (.
	ani	05fh		; toupper
	call	L01e1		;; 0260: cd e1 01    ...
	mvi	a,027h		;; 0263: 3e 27       >'
L0265:	call	L01e1		;; 0265: cd e1 01    ...
	pop	h		;; 0268: e1          .
	pop	b		;; 0269: c1          .
	ret			;; 026a: c9          .

L026b:	call	L01ce		; input
	call	L01e1		; echo
	mov	e,a		;; 0271: 5f          _
	call	L0297		; validate
	call	L01ce		; input
	call	L01e1		; echo
	cpi	027h		; check '
	jrnz	L028b		;; 027d: 20 0c        .
	mov	a,e		;; 027f: 7b          {
	ori	020h		; tolower
	call	L0297		; validate
	call	L01ce		; input
	call	L01e1		; echo
L028b:	cpi	'/'		;; 028b: fe 2f       ./
	jnz	L00ef		; error...
	lxi	b,L1c26		;; 0290: 01 26 1c    .&.
	dad	b		; HL=validation table?
	jmp	L011f		;; 0294: c3 1f 01    ...

L0297:	lxi	h,L0399		;; 0297: 21 99 03    ...
	lxi	b,25		;; 029a: 01 19 00    ...
	ccir			;; 029d: ed b1       ..
	rz			;; 029f: c8          .
	jmp	L00ef		;; 02a0: c3 ef 00    ...

L02a3:	shld	L1fda		;; 02a3: 22 da 1f    "..
	pop	h		;; 02a6: e1          .
	shld	L1fd8		;; 02a7: 22 d8 1f    "..
	sspd	L1fd6		;; 02aa: ed 73 d6 1f .s..
	lxi	sp,L1fd6	;; 02ae: 31 d6 1f    1..
	push	psw		;; 02b1: f5          .
	ldai			;; 02b2: ed 57       .W
	di			;; 02b4: f3          .
	mvi	l,0c9h		; RET
	jpo	L02bd		;; 02b7: e2 bd 02    ...
	lxi	h,0c9fbh	; EI; RET
L02bd:	shld	L1fdc		; patch return-to-user
	; save registers
	mov	h,a		;; 02c0: 67          g
	ldar			;; 02c1: ed 5f       ._
	mov	l,a		;; 02c3: 6f          o
	pop	psw		;; 02c4: f1          .
	pushiy			;; 02c5: fd e5       ..
	pushix			;; 02c7: dd e5       ..
	push	h		;; 02c9: e5          .
	exaf			;; 02ca: 08          .
	exx			;; 02cb: d9          .
	push	h		;; 02cc: e5          .
	push	d		;; 02cd: d5          .
	push	b		;; 02ce: c5          .
	push	psw		;; 02cf: f5          .
	exaf			;; 02d0: 08          .
	exx			;; 02d1: d9          .
	lhld	L1fda		;; 02d2: 2a da 1f    *..
	push	h		;; 02d5: e5          .
	push	d		;; 02d6: d5          .
	push	b		;; 02d7: c5          .
	push	psw		;; 02d8: f5          .
	call	L01c4		;; 02d9: cd c4 01    ...
	mvi	a,'>'		;; 02dc: 3e 3e       >>
	call	L01e1		;; 02de: cd e1 01    ...
	lhld	L1fd8		;; 02e1: 2a d8 1f    *..
	call	L0215		;; 02e4: cd 15 02    ...
	jmp	L0094		;; 02e7: c3 94 00    ...

; Restore registers? "return to program"?
L02ea:	xra	a		;; 02ea: af          .
	sta	L1ffc		;; 02eb: 32 fc 1f    2..
	pop	psw		;; 02ee: f1          .
	pop	b		;; 02ef: c1          .
	pop	d		;; 02f0: d1          .
	pop	h		;; 02f1: e1          .
	shld	L1fda		;; 02f2: 22 da 1f    "..
	exaf			;; 02f5: 08          .
	exx			;; 02f6: d9          .
	pop	psw		;; 02f7: f1          .
	pop	b		;; 02f8: c1          .
	pop	d		;; 02f9: d1          .
	pop	h		;; 02fa: e1          .
	exaf			;; 02fb: 08          .
	exx			;; 02fc: d9          .
	pop	h		;; 02fd: e1          .
	push	psw		;; 02fe: f5          .
	mov	a,h		;; 02ff: 7c          |
	; lost code...
	ret			;; 0300: c9          .
	ret			;; 0301: c9          .
	ret			;; 0302: c9          .
	ret			;; 0303: c9          .
	ret			;; 0304: c9          .
	ret			;; 0305: c9          .
	ret			;; 0306: c9          .
	ret			;; 0307: c9          .
	ret			;; 0308: c9          .
	ret			;; 0309: c9          .
	ret			;; 030a: c9          .
	ret			;; 030b: c9          .
	ret			;; 030c: c9          .
	ret			;; 030d: c9          .
	ret			;; 030e: c9          .
	ret			;; 030f: c9          .
	ret			;; 0310: c9          .
	ret			;; 0311: c9          .
	ret			;; 0312: c9          .
	ret			;; 0313: c9          .
	ret			;; 0314: c9          .
	ret			;; 0315: c9          .
	ret			;; 0316: c9          .
	ret			;; 0317: c9          .
	ret			;; 0318: c9          .
	ret			;; 0319: c9          .
	ret			;; 031a: c9          .
	ret			;; 031b: c9          .
	ret			;; 031c: c9          .
	ret			;; 031d: c9          .
	ret			;; 031e: c9          .
	ret			;; 031f: c9          .
	ret			;; 0320: c9          .
	ret			;; 0321: c9          .
	ret			;; 0322: c9          .
	ret			;; 0323: c9          .
	ret			;; 0324: c9          .
	ret			;; 0325: c9          .
	ret			;; 0326: c9          .
	ret			;; 0327: c9          .
	ret			;; 0328: c9          .
	ret			;; 0329: c9          .
	ret			;; 032a: c9          .
;
L032b:	ret			;; 032b: c9          .
	ret			;; 032c: c9          .
	ret			;; 032d: c9          .
	ret			;; 032e: c9          .
	ret			;; 032f: c9          .
	ret			;; 0330: c9          .
	ret			;; 0331: c9          .
	ret			;; 0332: c9          .
	ret			;; 0333: c9          .
	ret			;; 0334: c9          .
	ret			;; 0335: c9          .
	ret			;; 0336: c9          .
	ret			;; 0337: c9          .
	ret			;; 0338: c9          .
	ret			;; 0339: c9          .
	ret			;; 033a: c9          .
	ret			;; 033b: c9          .
	ret			;; 033c: c9          .
	ret			;; 033d: c9          .
	ret			;; 033e: c9          .
	ret			;; 033f: c9          .
	ret			;; 0340: c9          .
	ret			;; 0341: c9          .
	ret			;; 0342: c9          .
	ret			;; 0343: c9          .
	ret			;; 0344: c9          .
	ret			;; 0345: c9          .
	ret			;; 0346: c9          .
	ret			;; 0347: c9          .
	ret			;; 0348: c9          .
	ret			;; 0349: c9          .
	ret			;; 034a: c9          .
	ret			;; 034b: c9          .
	ret			;; 034c: c9          .
	ret			;; 034d: c9          .
	ret			;; 034e: c9          .
	ret			;; 034f: c9          .
	ret			;; 0350: c9          .
	ret			;; 0351: c9          .
	ret			;; 0352: c9          .
	ret			;; 0353: c9          .
	ret			;; 0354: c9          .
	ret			;; 0355: c9          .
	ret			;; 0356: c9          .
	ret			;; 0357: c9          .
	ret			;; 0358: c9          .
	ret			;; 0359: c9          .
	ret			;; 035a: c9          .
	ret			;; 035b: c9          .
	ret			;; 035c: c9          .
	ret			;; 035d: c9          .
	ret			;; 035e: c9          .
	ret			;; 035f: c9          .
	ret			;; 0360: c9          .
	ret			;; 0361: c9          .
	ret			;; 0362: c9          .
	ret			;; 0363: c9          .
	ret			;; 0364: c9          .
	ret			;; 0365: c9          .
	ret			;; 0366: c9          .
	ret			;; 0367: c9          .
	ret			;; 0368: c9          .
	ret			;; 0369: c9          .
	ret			;; 036a: c9          .
	ret			;; 036b: c9          .
	ret			;; 036c: c9          .
	ret			;; 036d: c9          .
	ret			;; 036e: c9          .
	ret			;; 036f: c9          .
	ret			;; 0370: c9          .
	ret			;; 0371: c9          .
	ret			;; 0372: c9          .
	ret			;; 0373: c9          .
	ret			;; 0374: c9          .
	ret			;; 0375: c9          .
	ret			;; 0376: c9          .
	ret			;; 0377: c9          .
	ret			;; 0378: c9          .
	ret			;; 0379: c9          .
	ret			;; 037a: c9          .
	ret			;; 037b: c9          .
	ret			;; 037c: c9          .
	ret			;; 037d: c9          .
	ret			;; 037e: c9          .
	ret			;; 037f: c9          .
	ret			;; 0380: c9          .
	ret			;; 0381: c9          .
	ret			;; 0382: c9          .
	ret			;; 0383: c9          .
	ret			;; 0384: c9          .
	ret			;; 0385: c9          .
	ret			;; 0386: c9          .
	ret			;; 0387: c9          .
	ret			;; 0388: c9          .
	ret			;; 0389: c9          .
	ret			;; 038a: c9          .
	ret			;; 038b: c9          .
	ret			;; 038c: c9          .
	ret			;; 038d: c9          .
	ret			;; 038e: c9          .
	ret			;; 038f: c9          .
	ret			;; 0390: c9          .
	ret			;; 0391: c9          .
	ret			;; 0392: c9          .
	ret			;; 0393: c9          .
	ret			;; 0394: c9          .
	ret			;; 0395: c9          .
	ret			;; 0396: c9          .
	ret			;; 0397: c9          .
	ret			;; 0398: c9          .

L0399:	;ds	25	; table for ccir at L0297 (valid input chars?)
	db	GONE,GONE,GONE,GONE,GONE,GONE,GONE,GONE,GONE,GONE
	db	GONE,GONE,GONE,GONE,GONE,GONE,GONE,GONE,GONE,GONE
	db	GONE,GONE,GONE,GONE,GONE
L03b2:	;ds	2*20	; I/O init table
	db	GONE,GONE,GONE,GONE,GONE,GONE,GONE,GONE,GONE,GONE
	db	GONE,GONE,GONE,GONE,GONE,GONE,GONE,GONE,GONE,GONE
	db	GONE,GONE,GONE,GONE,GONE,GONE,GONE,GONE,GONE,GONE
	db	GONE,GONE,GONE,GONE,GONE,GONE,GONE,GONE,GONE,GONE
L03da:	;ds	2*2	; optional I/O init
	db	GONE,GONE,GONE,GONE
	;ds	1		;; 03de: c9          .
	db	GONE

; missing routine for vector L004c (input line?)
L03df:	ret			;; 03df: c9          .
	ret			;; 03e0: c9          .
	ret			;; 03e1: c9          .
	ret			;; 03e2: c9          .
	ret			;; 03e3: c9          .
	ret			;; 03e4: c9          .
	ret			;; 03e5: c9          .
L03e6:	ret			;; 03e6: c9          .
	ret			;; 03e7: c9          .
	ret			;; 03e8: c9          .
	ret			;; 03e9: c9          .
	ret			;; 03ea: c9          .
L03eb:	ret			;; 03eb: c9          .
	ret			;; 03ec: c9          .
	ret			;; 03ed: c9          .
	ret			;; 03ee: c9          .
	ret			;; 03ef: c9          .
	ret			;; 03f0: c9          .
	ret			;; 03f1: c9          .
	ret			;; 03f2: c9          .
	ret			;; 03f3: c9          .
	ret			;; 03f4: c9          .
	ret			;; 03f5: c9          .
	ret			;; 03f6: c9          .
	ret			;; 03f7: c9          .
	ret			;; 03f8: c9          .
	ret			;; 03f9: c9          .
	ret			;; 03fa: c9          .
	ret			;; 03fb: c9          .
	ret			;; 03fc: c9          .
	ret			;; 03fd: c9          .
	ret			;; 03fe: c9          .
	ret			;; 03ff: c9          .

	cpi	01dh		;; 0400: fe 1d       ..
	jrz	L043c		;; 0402: 28 38       (8
	out	sioBdat		;; 0404: d3 05       ..
	jr	L03e6		;; 0406: 18 de       ..

L0408:	in	sioBctl		;; 0408: db 07       ..
	bit	0,a		; Rx Available
	jrz	L03eb		;; 040c: 28 dd       (.
	in	sioActl		;; 040e: db 06       ..
	bit	2,a		; Tx Pending
	jrz	L03eb		;; 0412: 28 d7       (.
	in	sioBdat		;; 0414: db 05       ..
	bitx	1,+0		;; 0416: dd cb 00 4e ...N
	jrz	L0438		;; 041a: 28 1c       (.
	bitx	7,+0		;; 041c: dd cb 00 7e ...~
	jrz	L0426		;; 0420: 28 04       (.
	cpi	':'		;; 0422: fe 3a       .:
	jrz	L045e		;; 0424: 28 38       (8
L0426:	cpi	LF		;; 0426: fe 0a       ..
	jrz	L0434		;; 0428: 28 0a       (.
	cpi	CR		;; 042a: fe 0d       ..
	jrz	L0434		;; 042c: 28 06       (.
	resx	7,+0		;; 042e: dd cb 00 be ....
	jr	L0438		;; 0432: 18 04       ..

L0434:	setx	7,+0		;; 0434: dd cb 00 fe ....
L0438:	out	sioAdat		;; 0438: d3 04       ..
	jr	L03eb		;; 043a: 18 af       ..

; Send BREAK on ch B
L043c:	dcrx	+1		;; 043c: dd 35 01    .5.
	jrnz	L03eb		;; 043f: 20 aa        .
	mvi	a,005h		;; 0441: 3e 05       >.
	out	sioBctl		;; 0443: d3 07       ..
	mvi	a,078h		; 8-bit, BREAK, TxEn
	out	sioBctl		;; 0447: d3 07       ..
	push	b		;; 0449: c5          .
	mvi	b,0		;; 044a: 06 00       ..
	mov	c,b		;; 044c: 48          H
L044d:	inr	c		;; 044d: 0c          .
	jrnz	L044d		;; 044e: 20 fd        .
	inr	b		;; 0450: 04          .
	jrnz	L044d		;; 0451: 20 fa        .
	pop	b		;; 0453: c1          .
	mvi	a,005h		;; 0454: 3e 05       >.
	out	sioBctl		;; 0456: d3 07       ..
	mvi	a,068h		; 8-bit, TxEn (BREAK off)
	out	sioBctl		;; 045a: d3 07       ..
	jr	L03e6		;; 045c: 18 88       ..

L045e:	call	L0463		;; 045e: cd 63 04    .c.
	jr	L03e6		;; 0461: 18 83       ..

L0463:	push	psw		;; 0463: f5          .
	push	b		;; 0464: c5          .
	push	d		;; 0465: d5          .
	push	h		;; 0466: e5          .
	mvi	c,007h		;; 0467: 0e 07       ..
	xra	a		;; 0469: af          .
	jmp	L032b		;; 046a: c3 2b 03    .+.

				; 046d: ff ...
	db	0ffh,0ffh,0ffh,0ffh,0ffh,0ffh,0ffh,0ffh
	db	0ffh,0ffh,0ffh,0ffh,0ffh,0ffh,0ffh,0ffh
	db	0ffh,0ffh,0ffh	; ... 047f: ff          .

; 'H' command - input octal digits...
L0480:	mvi	a,00fh		; PIO output mode
	out	pio1Ac		;; 0482: d3 0a       ..
	out	pio1Bc		;; 0484: d3 0b       ..
	out	pio2Ac		;; 0486: d3 0e       ..
	mvi	a,0cfh		; PIO control mode
	out	pio2Bc		;; 048a: d3 0f       ..
	mvi	a,00fh		; O O O O I I I I
	out	pio2Bc		;; 048e: d3 0f       ..
	lxiy	L07d6		;; 0490: fd 21 d6 07 ....
L0494:	call	L0049		;; 0494: cd 49 00    .I.
	mvi	a,'_'		;; 0497: 3e 5f       >_
	call	L0046		;; 0499: cd 46 00    .F.
	mvi	c,000h		;; 049c: 0e 00       ..
	res	0,c		;; 049e: cb 81       ..
	lxi	h,0		;; 04a0: 21 00 00    ...
L04a3:	call	L05e3		;; 04a3: cd e3 05    ...
	cpi	'0'		;; 04a6: fe 30       .0
	jrc	L04bc		;; 04a8: 38 12       8.
	cpi	'7'+1		;; 04aa: fe 38       .8
	jrnc	L04bc		;; 04ac: 30 0e       0.
	call	L0046		;; 04ae: cd 46 00    .F.
	sui	'0'		;; 04b1: d6 30       .0
	dad	h		;; 04b3: 29          )
	dad	h		;; 04b4: 29          )
	dad	h		;; 04b5: 29          )
	add	l		;; 04b6: 85          .
	mov	l,a		;; 04b7: 6f          o
	setb	0,c		;; 04b8: cb c1       ..
	jr	L04a3		;; 04ba: 18 e7       ..

L04bc:	cpi	CR		;; 04bc: fe 0d       ..
	jrz	L0523		;; 04be: 28 63       (c
	cpi	'.'		;; 04c0: fe 2e       ..
	jrz	L052b		;; 04c2: 28 67       (g
	cpi	','		;; 04c4: fe 2c       .,
	jrz	L0538		;; 04c6: 28 70       (p
	cpi	'\'		;; 04c8: fe 5c       .\
	jz	L055f		;; 04ca: ca 5f 05    ._.
	cpi	LF		;; 04cd: fe 0a       ..
	jrz	L0548		;; 04cf: 28 77       (w
	call	L0046		; echo
	cpi	'S'		;; 04d4: fe 53       .S
	jrz	L0512		;; 04d6: 28 3a       (:
	cpi	'R'		;; 04d8: fe 52       .R
	jrz	L0517		;; 04da: 28 3b       (;
	cpi	'X'		;; 04dc: fe 58       .X
	jz	L0040		;; 04de: ca 40 00    .@.
	cpi	'M'		;; 04e1: fe 4d       .M
	jz	L068b		;; 04e3: ca 8b 06    ...
	cpi	'/'		;; 04e6: fe 2f       ./
	jz	L0588		;; 04e8: ca 88 05    ...
	cpi	'^'		;; 04eb: fe 5e       .^
	jrz	L0564		;; 04ed: 28 75       (u
	cpi	'>'		;; 04ef: fe 3e       .>
	jrz	L0530		;; 04f1: 28 3d       (=
	cpi	'<'		;; 04f3: fe 3c       .<
	jrz	L053d		;; 04f5: 28 46       (F
	cpi	'G'		;; 04f7: fe 47       .G
	jz	L0606		;; 04f9: ca 06 06    ...
	cpi	'P'		;; 04fc: fe 50       .P
	jz	(GONE SHL 8)+2bh		;; 04fe: ca 2b c9    .+.

	ret			;; 0501: c9          .
	ret			;; 0502: c9          .
	ret			;; 0503: c9          .
	ret			;; 0504: c9          .
	ret			;; 0505: c9          .
	ret			;; 0506: c9          .
	ret			;; 0507: c9          .
	ret			;; 0508: c9          .
	ret			;; 0509: c9          .
	ret			;; 050a: c9          .
L050b:	ret			;; 050b: c9          .
	ret			;; 050c: c9          .
	ret			;; 050d: c9          .
	ret			;; 050e: c9          .
	ret			;; 050f: c9          .
	ret			;; 0510: c9          .
	ret			;; 0511: c9          .

; handle 'S'
L0512:	ret			;; 0512: c9          .
	ret			;; 0513: c9          .
	ret			;; 0514: c9          .
	ret			;; 0515: c9          .
	ret			;; 0516: c9          .

; handle 'R'
L0517:	ret			;; 0517: c9          .
	ret			;; 0518: c9          .
	ret			;; 0519: c9          .
	ret			;; 051a: c9          .
	ret			;; 051b: c9          .
	ret			;; 051c: c9          .
	ret			;; 051d: c9          .
	ret			;; 051e: c9          .
	ret			;; 051f: c9          .
	ret			;; 0520: c9          .
	ret			;; 0521: c9          .
	ret			;; 0522: c9          .

; handle CR
L0523:	ret			;; 0523: c9          .
	ret			;; 0524: c9          .
	ret			;; 0525: c9          .
	ret			;; 0526: c9          .
	ret			;; 0527: c9          .
	ret			;; 0528: c9          .
	ret			;; 0529: c9          .
	ret			;; 052a: c9          .

; handle '.'
L052b:	ret			;; 052b: c9          .
	ret			;; 052c: c9          .
	ret			;; 052d: c9          .
	ret			;; 052e: c9          .
	ret			;; 052f: c9          .

L0530:	ret			;; 0530: c9          .
	ret			;; 0531: c9          .
	ret			;; 0532: c9          .
	ret			;; 0533: c9          .
	ret			;; 0534: c9          .
	ret			;; 0535: c9          .
	ret			;; 0536: c9          .
	ret			;; 0537: c9          .

; handle ','
L0538:	ret			;; 0538: c9          .
	ret			;; 0539: c9          .
	ret			;; 053a: c9          .
	ret			;; 053b: c9          .
	ret			;; 053c: c9          .
L053d:	ret			;; 053d: c9          .
	ret			;; 053e: c9          .
	ret			;; 053f: c9          .
	ret			;; 0540: c9          .
	ret			;; 0541: c9          .
	ret			;; 0542: c9          .
	ret			;; 0543: c9          .
	ret			;; 0544: c9          .
	ret			;; 0545: c9          .
	ret			;; 0546: c9          .
	ret			;; 0547: c9          .

; handle LF
L0548:	ret			;; 0548: c9          .
	ret			;; 0549: c9          .
	ret			;; 054a: c9          .
	ret			;; 054b: c9          .
	ret			;; 054c: c9          .
	ret			;; 054d: c9          .
	ret			;; 054e: c9          .
	ret			;; 054f: c9          .
	ret			;; 0550: c9          .
	ret			;; 0551: c9          .
	ret			;; 0552: c9          .
	ret			;; 0553: c9          .
	ret			;; 0554: c9          .
	ret			;; 0555: c9          .
	ret			;; 0556: c9          .
	ret			;; 0557: c9          .
	ret			;; 0558: c9          .
	ret			;; 0559: c9          .
	ret			;; 055a: c9          .
	ret			;; 055b: c9          .
	ret			;; 055c: c9          .
	ret			;; 055d: c9          .
	ret			;; 055e: c9          .

; handle '\'
L055f:	ret			;; 055f: c9          .
	ret			;; 0560: c9          .
	ret			;; 0561: c9          .
	ret			;; 0562: c9          .
	ret			;; 0563: c9          .
L0564:	ret			;; 0564: c9          .
	ret			;; 0565: c9          .
	ret			;; 0566: c9          .
	ret			;; 0567: c9          .
	ret			;; 0568: c9          .
	ret			;; 0569: c9          .
	ret			;; 056a: c9          .
	ret			;; 056b: c9          .
	ret			;; 056c: c9          .
	ret			;; 056d: c9          .
	ret			;; 056e: c9          .
	ret			;; 056f: c9          .
	ret			;; 0570: c9          .
	ret			;; 0571: c9          .
	ret			;; 0572: c9          .
	ret			;; 0573: c9          .
	ret			;; 0574: c9          .
	ret			;; 0575: c9          .
	ret			;; 0576: c9          .
	ret			;; 0577: c9          .
	ret			;; 0578: c9          .
	ret			;; 0579: c9          .
	ret			;; 057a: c9          .
	ret			;; 057b: c9          .
	ret			;; 057c: c9          .
	ret			;; 057d: c9          .
	ret			;; 057e: c9          .
	ret			;; 057f: c9          .
	ret			;; 0580: c9          .
	ret			;; 0581: c9          .
	ret			;; 0582: c9          .
	ret			;; 0583: c9          .
	ret			;; 0584: c9          .
	ret			;; 0585: c9          .
	ret			;; 0586: c9          .
	ret			;; 0587: c9          .
L0588:	ret			;; 0588: c9          .
	ret			;; 0589: c9          .
	ret			;; 058a: c9          .
	ret			;; 058b: c9          .
	ret			;; 058c: c9          .
	ret			;; 058d: c9          .
	ret			;; 058e: c9          .
	ret			;; 058f: c9          .
	ret			;; 0590: c9          .
	ret			;; 0591: c9          .
	ret			;; 0592: c9          .
	ret			;; 0593: c9          .
	ret			;; 0594: c9          .
	ret			;; 0595: c9          .
	ret			;; 0596: c9          .
	ret			;; 0597: c9          .
	ret			;; 0598: c9          .
	ret			;; 0599: c9          .
	ret			;; 059a: c9          .
	ret			;; 059b: c9          .
	ret			;; 059c: c9          .
	ret			;; 059d: c9          .
	ret			;; 059e: c9          .
	ret			;; 059f: c9          .
	ret			;; 05a0: c9          .
	ret			;; 05a1: c9          .
	ret			;; 05a2: c9          .
	ret			;; 05a3: c9          .
	ret			;; 05a4: c9          .
	ret			;; 05a5: c9          .
	ret			;; 05a6: c9          .
	ret			;; 05a7: c9          .
	ret			;; 05a8: c9          .
	ret			;; 05a9: c9          .
	ret			;; 05aa: c9          .
	ret			;; 05ab: c9          .
	ret			;; 05ac: c9          .
	ret			;; 05ad: c9          .
	ret			;; 05ae: c9          .
	ret			;; 05af: c9          .
	ret			;; 05b0: c9          .
	ret			;; 05b1: c9          .
	ret			;; 05b2: c9          .
	ret			;; 05b3: c9          .
	ret			;; 05b4: c9          .
	ret			;; 05b5: c9          .
	ret			;; 05b6: c9          .
	ret			;; 05b7: c9          .
	ret			;; 05b8: c9          .
	ret			;; 05b9: c9          .
	ret			;; 05ba: c9          .
	ret			;; 05bb: c9          .
	ret			;; 05bc: c9          .
	ret			;; 05bd: c9          .
	ret			;; 05be: c9          .
L05bf:	ret			;; 05bf: c9          .
	ret			;; 05c0: c9          .
	ret			;; 05c1: c9          .
	ret			;; 05c2: c9          .
	ret			;; 05c3: c9          .
	ret			;; 05c4: c9          .
	ret			;; 05c5: c9          .
	ret			;; 05c6: c9          .
	ret			;; 05c7: c9          .
	ret			;; 05c8: c9          .
	ret			;; 05c9: c9          .
	ret			;; 05ca: c9          .
	ret			;; 05cb: c9          .
	ret			;; 05cc: c9          .
	ret			;; 05cd: c9          .
	ret			;; 05ce: c9          .
	ret			;; 05cf: c9          .
	ret			;; 05d0: c9          .
	ret			;; 05d1: c9          .
	ret			;; 05d2: c9          .
	ret			;; 05d3: c9          .
	ret			;; 05d4: c9          .
	ret			;; 05d5: c9          .
	ret			;; 05d6: c9          .
	ret			;; 05d7: c9          .
	ret			;; 05d8: c9          .
	ret			;; 05d9: c9          .
	ret			;; 05da: c9          .
	ret			;; 05db: c9          .
	ret			;; 05dc: c9          .
	ret			;; 05dd: c9          .
	ret			;; 05de: c9          .
	ret			;; 05df: c9          .
	ret			;; 05e0: c9          .
	ret			;; 05e1: c9          .
	ret			;; 05e2: c9          .
L05e3:	ret			;; 05e3: c9          .
	ret			;; 05e4: c9          .
	ret			;; 05e5: c9          .
	ret			;; 05e6: c9          .
	ret			;; 05e7: c9          .
	ret			;; 05e8: c9          .
	ret			;; 05e9: c9          .
	ret			;; 05ea: c9          .
	ret			;; 05eb: c9          .
	ret			;; 05ec: c9          .
	ret			;; 05ed: c9          .
L05ee:	ret			;; 05ee: c9          .
	ret			;; 05ef: c9          .
	ret			;; 05f0: c9          .
	ret			;; 05f1: c9          .
	ret			;; 05f2: c9          .
	ret			;; 05f3: c9          .
	ret			;; 05f4: c9          .
	ret			;; 05f5: c9          .
	ret			;; 05f6: c9          .
	ret			;; 05f7: c9          .
	ret			;; 05f8: c9          .
	ret			;; 05f9: c9          .
	ret			;; 05fa: c9          .
	ret			;; 05fb: c9          .
	ret			;; 05fc: c9          .
	ret			;; 05fd: c9          .
	ret			;; 05fe: c9          .
	ret			;; 05ff: c9          .

	rlc			;; 0600: 07          .
	out	pio1Ad		;; 0601: d3 08       ..
	pop	b		;; 0603: c1          .
	pop	psw		;; 0604: f1          .
	ret			;; 0605: c9          .

L0606:	call	L05bf		;; 0606: cd bf 05    ...
	lxi	d,L0000		;; 0609: 11 00 00    ...
	lxi	h,L1600		;; 060c: 21 00 16    ...
	lxi	b,0f800h	;; 060f: 01 00 f8    ...
L0612:	call	L05ee		;; 0612: cd ee 05    ...
	in	pio2Bd		;; 0615: db 0d       ..
	mov	m,a		;; 0617: 77          w
	inx	d		;; 0618: 13          .
	call	L05ee		;; 0619: cd ee 05    ...
	in	pio2Bd		;; 061c: db 0d       ..
	rld			;; 061e: ed 6f       .o
	inx	d		;; 0620: 13          .
	inx	h		;; 0621: 23          #
	inr	c		;; 0622: 0c          .
	jrnz	L0612		;; 0623: 20 ed        .
	inr	b		;; 0625: 04          .
	jrnz	L0612		;; 0626: 20 ea        .
	jmp	L0494		;; 0628: c3 94 04    ...

	call	L05bf		;; 062b: cd bf 05    ...
	lxi	d,L0000		;; 062e: 11 00 00    ...
	lxi	h,L1600		;; 0631: 21 00 16    ...
	lxi	b,0f800h	;; 0634: 01 00 f8    ...
L0637:	call	L05ee		;; 0637: cd ee 05    ...
	mov	a,m		;; 063a: 7e          ~
	rrc			;; 063b: 0f          .
	rrc			;; 063c: 0f          .
	rrc			;; 063d: 0f          .
	rrc			;; 063e: 0f          .
	out	pio2Ad		;; 063f: d3 0c       ..
	inx	d		;; 0641: 13          .
	call	L05ee		;; 0642: cd ee 05    ...
	mov	a,m		;; 0645: 7e          ~
	out	pio2Ad		;; 0646: d3 0c       ..
	inx	d		;; 0648: 13          .
	inx	h		;; 0649: 23          #
	inr	c		;; 064a: 0c          .
	jrnz	L0637		;; 064b: 20 ea        .
	inr	b		;; 064d: 04          .
	jrnz	L0637		;; 064e: 20 e7        .
	jmp	L0494		;; 0650: c3 94 04    ...

	mvi	a,002h		;; 0653: 3e 02       >.
	call	L004c		;; 0655: cd 4c 00    .L.
	jmp	L0494		;; 0658: c3 94 04    ...

	call	L05bf		;; 065b: cd bf 05    ...
	mvi	c,pio2Bd	;; 065e: 0e 0d       ..
	xra	a		;; 0660: af          .
	outp	a		;; 0661: ed 79       .y
	mov	a,h		;; 0663: 7c          |
	ani	00fh		;; 0664: e6 0f       ..
	jrnz	L066e		;; 0666: 20 06        .
	mov	a,l		;; 0668: 7d          }
	cpi	004h		;; 0669: fe 04       ..
	jc	L050b		;; 066b: da 0b 05    ...
L066e:	dcx	h		;; 066e: 2b          +
	dcx	h		;; 066f: 2b          +
	mov	a,l		;; 0670: 7d          }
	xri	001h		;; 0671: ee 01       ..
	out	pio1Ad		;; 0673: d3 08       ..
	mvi	a,010h		; set line PB4
	outp	a		;; 0677: ed 79       .y
	xra	a		; clear line PB4
	outp	a		;; 067a: ed 79       .y
	dad	h		;; 067c: 29          )
	dad	h		;; 067d: 29          )
	mov	a,h		;; 067e: 7c          |
	out	pio1Ad		;; 067f: d3 08       ..
	mvi	a,020h		; set line PB5
	outp	a		;; 0683: ed 79       .y
	xra	a		;; 0685: af          .
	outp	a		; clear line PB5
	jmp	L0494		;; 0688: c3 94 04    ...

L068b:	call	L05bf		;; 068b: cd bf 05    ...
	mov	a,h		;; 068e: 7c          |
	ora	l		;; 068f: b5          .
	jrnz	L0695		;; 0690: 20 03        .
	lxi	h,1		;; 0692: 21 01 00    ...
L0695:	push	h		;; 0695: e5          .
	xra	a		;; 0696: af          .
	out	pio1Ad		;; 0697: d3 08       ..
	mov	c,a		;; 0699: 4f          O
L069a:	out	pio1Bd		;; 069a: d3 09       ..
	xra	a		;; 069c: af          .
	out	pio2Ad		;; 069d: d3 0c       ..
	in	pio1Bd		;; 069f: db 09       ..
	inr	a		;; 06a1: 3c          <
	cmpy	+0		;; 06a2: fd be 00    ...
	jrc	L069a		;; 06a5: 38 f3       8.
L06a7:	mvi	b,001h		;; 06a7: 06 01       ..
L06a9:	mov	a,c		;; 06a9: 79          y
	out	pio1Bd		;; 06aa: d3 09       ..
	mov	a,b		;; 06ac: 78          x
	out	pio2Ad		;; 06ad: d3 0c       ..
	xra	a		;; 06af: af          .
L06b0:	out	pio1Bd		;; 06b0: d3 09       ..
	mvi	d,000h		;; 06b2: 16 00       ..
	cmp	c		;; 06b4: b9          .
	jrnz	L06b8		;; 06b5: 20 01        .
	mov	d,b		;; 06b7: 50          P
L06b8:	in	pio2Bd		;; 06b8: db 0d       ..
	cmp	d		;; 06ba: ba          .
	jrz	L06da		;; 06bb: 28 1d       (.
	call	L0049		;; 06bd: cd 49 00    .I.
	mvi	a,'D'		;; 06c0: 3e 44       >D
	call	L0046		;; 06c2: cd 46 00    .F.
	in	pio1Bd		;; 06c5: db 09       ..
	call	L07b3		;; 06c7: cd b3 07    ...
	mov	a,c		;; 06ca: 79          y
	call	L07b3		;; 06cb: cd b3 07    ...
	in	pio2Bd		;; 06ce: db 0d       ..
	call	L07b3		;; 06d0: cd b3 07    ...
	mov	a,b		;; 06d3: 78          x
	call	L07b3		;; 06d4: cd b3 07    ...
	call	L07c8		;; 06d7: cd c8 07    ...
L06da:	in	pio1Bd		;; 06da: db 09       ..
	inr	a		;; 06dc: 3c          <
	cmpy	+0		;; 06dd: fd be 00    ...
	jrc	L06b0		;; 06e0: 38 ce       8.
	slar	b		;; 06e2: cb 20       . 
	mov	a,b		;; 06e4: 78          x
	ani	00fh		;; 06e5: e6 0f       ..
	jrnz	L06a9		;; 06e7: 20 c0        .
	mov	a,c		;; 06e9: 79          y
	out	pio1Bd		;; 06ea: d3 09       ..
	xra	a		;; 06ec: af          .
	out	pio2Ad		;; 06ed: d3 0c       ..
	inr	c		;; 06ef: 0c          .
	mov	a,c		;; 06f0: 79          y
	cmpy	+0		;; 06f1: fd be 00    ...
	jrc	L06a7		;; 06f4: 38 b1       8.
	xra	a		;; 06f6: af          .
	out	pio1Bd		;; 06f7: d3 09       ..
	mvi	b,001h		;; 06f9: 06 01       ..
	xra	a		;; 06fb: af          .
	out	pio1Ad		;; 06fc: d3 08       ..
	mov	a,b		;; 06fe: 78          x
	out	GONE		;; 06ff: d3 c9       ..
	ret			;; 0701: c9          .
	ret			;; 0702: c9          .
	ret			;; 0703: c9          .
	ret			;; 0704: c9          .
	ret			;; 0705: c9          .
	ret			;; 0706: c9          .
	ret			;; 0707: c9          .
	ret			;; 0708: c9          .
	ret			;; 0709: c9          .
	ret			;; 070a: c9          .
	ret			;; 070b: c9          .
	ret			;; 070c: c9          .
	ret			;; 070d: c9          .
	ret			;; 070e: c9          .
	ret			;; 070f: c9          .
	ret			;; 0710: c9          .
	ret			;; 0711: c9          .
	ret			;; 0712: c9          .
	ret			;; 0713: c9          .
	ret			;; 0714: c9          .
	ret			;; 0715: c9          .
	ret			;; 0716: c9          .
	ret			;; 0717: c9          .
	ret			;; 0718: c9          .
	ret			;; 0719: c9          .
	ret			;; 071a: c9          .
	ret			;; 071b: c9          .
	ret			;; 071c: c9          .
	ret			;; 071d: c9          .
	ret			;; 071e: c9          .
	ret			;; 071f: c9          .
	ret			;; 0720: c9          .
	ret			;; 0721: c9          .
	ret			;; 0722: c9          .
	ret			;; 0723: c9          .
	ret			;; 0724: c9          .
	ret			;; 0725: c9          .
	ret			;; 0726: c9          .
	ret			;; 0727: c9          .
	ret			;; 0728: c9          .
	ret			;; 0729: c9          .
	ret			;; 072a: c9          .
	ret			;; 072b: c9          .
	ret			;; 072c: c9          .
	ret			;; 072d: c9          .
	ret			;; 072e: c9          .
	ret			;; 072f: c9          .
	ret			;; 0730: c9          .
	ret			;; 0731: c9          .
	ret			;; 0732: c9          .
	ret			;; 0733: c9          .
	ret			;; 0734: c9          .
	ret			;; 0735: c9          .
	ret			;; 0736: c9          .
	ret			;; 0737: c9          .
	ret			;; 0738: c9          .
	ret			;; 0739: c9          .
	ret			;; 073a: c9          .
	ret			;; 073b: c9          .
	ret			;; 073c: c9          .
	ret			;; 073d: c9          .
	ret			;; 073e: c9          .
	ret			;; 073f: c9          .
	ret			;; 0740: c9          .
	ret			;; 0741: c9          .
	ret			;; 0742: c9          .
	ret			;; 0743: c9          .
	ret			;; 0744: c9          .
	ret			;; 0745: c9          .
	ret			;; 0746: c9          .
	ret			;; 0747: c9          .
	ret			;; 0748: c9          .
	ret			;; 0749: c9          .
	ret			;; 074a: c9          .
	ret			;; 074b: c9          .
	ret			;; 074c: c9          .
	ret			;; 074d: c9          .
	ret			;; 074e: c9          .
	ret			;; 074f: c9          .
	ret			;; 0750: c9          .
	ret			;; 0751: c9          .
	ret			;; 0752: c9          .
	ret			;; 0753: c9          .
	ret			;; 0754: c9          .
	ret			;; 0755: c9          .
	ret			;; 0756: c9          .
	ret			;; 0757: c9          .
	ret			;; 0758: c9          .
	ret			;; 0759: c9          .
	ret			;; 075a: c9          .
	ret			;; 075b: c9          .
	ret			;; 075c: c9          .
	ret			;; 075d: c9          .
	ret			;; 075e: c9          .
	ret			;; 075f: c9          .
	ret			;; 0760: c9          .
	ret			;; 0761: c9          .
	ret			;; 0762: c9          .
	ret			;; 0763: c9          .
	ret			;; 0764: c9          .
	ret			;; 0765: c9          .
	ret			;; 0766: c9          .
	ret			;; 0767: c9          .
	ret			;; 0768: c9          .
	ret			;; 0769: c9          .
	ret			;; 076a: c9          .
	ret			;; 076b: c9          .
	ret			;; 076c: c9          .
	ret			;; 076d: c9          .
	ret			;; 076e: c9          .
	ret			;; 076f: c9          .
	ret			;; 0770: c9          .
	ret			;; 0771: c9          .
	ret			;; 0772: c9          .
	ret			;; 0773: c9          .
	ret			;; 0774: c9          .
	ret			;; 0775: c9          .
	ret			;; 0776: c9          .
	ret			;; 0777: c9          .
	ret			;; 0778: c9          .
	ret			;; 0779: c9          .
	ret			;; 077a: c9          .
	ret			;; 077b: c9          .
	ret			;; 077c: c9          .
	ret			;; 077d: c9          .
	ret			;; 077e: c9          .
	ret			;; 077f: c9          .
	ret			;; 0780: c9          .
	ret			;; 0781: c9          .
	ret			;; 0782: c9          .
	ret			;; 0783: c9          .
	ret			;; 0784: c9          .
	ret			;; 0785: c9          .
	ret			;; 0786: c9          .
	ret			;; 0787: c9          .
	ret			;; 0788: c9          .
	ret			;; 0789: c9          .
	ret			;; 078a: c9          .
	ret			;; 078b: c9          .
	ret			;; 078c: c9          .
	ret			;; 078d: c9          .
	ret			;; 078e: c9          .
	ret			;; 078f: c9          .
	ret			;; 0790: c9          .
	ret			;; 0791: c9          .
	ret			;; 0792: c9          .
	ret			;; 0793: c9          .
	ret			;; 0794: c9          .
	ret			;; 0795: c9          .
	ret			;; 0796: c9          .
	ret			;; 0797: c9          .
	ret			;; 0798: c9          .
	ret			;; 0799: c9          .
	ret			;; 079a: c9          .
	ret			;; 079b: c9          .
	ret			;; 079c: c9          .
	ret			;; 079d: c9          .
	ret			;; 079e: c9          .
	ret			;; 079f: c9          .
	ret			;; 07a0: c9          .
	ret			;; 07a1: c9          .
	ret			;; 07a2: c9          .
	ret			;; 07a3: c9          .
	ret			;; 07a4: c9          .
	ret			;; 07a5: c9          .
	ret			;; 07a6: c9          .
	ret			;; 07a7: c9          .
	ret			;; 07a8: c9          .
	ret			;; 07a9: c9          .
	ret			;; 07aa: c9          .
	ret			;; 07ab: c9          .
	ret			;; 07ac: c9          .
	ret			;; 07ad: c9          .
	ret			;; 07ae: c9          .
	ret			;; 07af: c9          .
	ret			;; 07b0: c9          .
	ret			;; 07b1: c9          .
	ret			;; 07b2: c9          .
L07b3:	ret			;; 07b3: c9          .
	ret			;; 07b4: c9          .
	ret			;; 07b5: c9          .
	ret			;; 07b6: c9          .
	ret			;; 07b7: c9          .
	ret			;; 07b8: c9          .
	ret			;; 07b9: c9          .
	ret			;; 07ba: c9          .
	ret			;; 07bb: c9          .
	ret			;; 07bc: c9          .
	ret			;; 07bd: c9          .
	ret			;; 07be: c9          .
	ret			;; 07bf: c9          .
	ret			;; 07c0: c9          .
	ret			;; 07c1: c9          .
	ret			;; 07c2: c9          .
	ret			;; 07c3: c9          .
	ret			;; 07c4: c9          .
	ret			;; 07c5: c9          .
	ret			;; 07c6: c9          .
	ret			;; 07c7: c9          .
L07c8:	ret			;; 07c8: c9          .
	ret			;; 07c9: c9          .
	ret			;; 07ca: c9          .
	ret			;; 07cb: c9          .
	ret			;; 07cc: c9          .
	ret			;; 07cd: c9          .
	ret			;; 07ce: c9          .
	ret			;; 07cf: c9          .
	ret			;; 07d0: c9          .
	ret			;; 07d1: c9          .
	ret			;; 07d2: c9          .
	ret			;; 07d3: c9          .
	ret			;; 07d4: c9          .
	ret			;; 07d5: c9          .
L07d6:	ret			;; 07d6: c9          .
	ret			;; 07d7: c9          .
	ret			;; 07d8: c9          .
	ret			;; 07d9: c9          .
	ret			;; 07da: c9          .
	ret			;; 07db: c9          .
	ret			;; 07dc: c9          .
	ret			;; 07dd: c9          .
	ret			;; 07de: c9          .
	ret			;; 07df: c9          .
	ret			;; 07e0: c9          .
	ret			;; 07e1: c9          .
	ret			;; 07e2: c9          .
	ret			;; 07e3: c9          .
	ret			;; 07e4: c9          .
	ret			;; 07e5: c9          .
	ret			;; 07e6: c9          .
	ret			;; 07e7: c9          .
	ret			;; 07e8: c9          .
	ret			;; 07e9: c9          .
	ret			;; 07ea: c9          .
	ret			;; 07eb: c9          .
	ret			;; 07ec: c9          .
	ret			;; 07ed: c9          .
	ret			;; 07ee: c9          .
	ret			;; 07ef: c9          .
	ret			;; 07f0: c9          .
	ret			;; 07f1: c9          .
	ret			;; 07f2: c9          .
	ret			;; 07f3: c9          .
	ret			;; 07f4: c9          .
	ret			;; 07f5: c9          .
	ret			;; 07f6: c9          .
	ret			;; 07f7: c9          .
	ret			;; 07f8: c9          .
	ret			;; 07f9: c9          .
	ret			;; 07fa: c9          .
	ret			;; 07fb: c9          .
	ret			;; 07fc: c9          .
	ret			;; 07fd: c9          .
	ret			;; 07fe: c9          .
	ret			;; 07ff: c9          .

	org	1600h
L1600:	ds	1574

L1c26:	ds	1
L1c27:	ds	889

L1fa0:	ds	32
L1fc0:	ds	22
L1fd6:	ds	2
L1fd8:	ds	2
L1fda:	ds	2
L1fdc:	ds	2
L1fde:	ds	29	; RST7 handler
L1ffb:	ds	1
L1ffc:	ds	4

	end
