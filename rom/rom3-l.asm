; EPROM fault on A8, lost all odd pages.
; Here, all odd pages have been filled with 0xc9 before disassembly.
GAP	macro	?A
	if	?A <> $
	.error	'gap snafu'
	endif
	endm
; User program breaks:
;	RST 1		save PC-1
;	NMI		(save PC)
;
; Top-level Monitor Commands (hex number prefix):
;	[addr]G		"Go" - resume program at <addr> or PC
;	P		"Go" at PC+1 (for after RST1?)
;	[addr]/		Print *(U16_t *)HL++ [L011f]
;	R<reg>[']/	Print 16-bit reg-pair value, reg={A,B,D,H,A',B',D',H',I,X,Y,S,P}
;	T		(top of stack dump? HL=savSP;'/') [L0127]
;	[addr]<CR>	print *(HL++) [L0112]
;	^		(opposite of 'V'?) [L0118]
;	V		(opposite of '^'?) [L012d]
;	I		(instr dump?) [L0151]
;	H		(remote control) (H)ost mode? [L0480]
;	<LF>		??? [L0109]
;	???		Terminal mode? [L03e6?]
;
; 'H' sub-commands (octal number prefix):
; (init PIOs, context L07d6)
;	.	(no echo) [L052b]
;	,	(no echo) [L0538]
;	\	(no echo) [L055f]
;	<LF>	(no echo) [L0548]
;	<CR>	(no echo) [L0523]
;	S	[L0512]
;	R	[L0517]
;	X	Exit program (return to monitor)
;	M	(something with PIO) [L068b]
;	/	[L0588]
;	^	[L0564]
;	>	[L0530]
;	<	[L053d]
;	G	Get 2K from PIO to piobuf [L0606]
;	P	Put 2K to PIO from piobuf [L062b]
;	two more??? [L0653, L065b, L068b? L03e6?]
;
	maclib	z80

NULL	equ	0ffh
GONE	equ	0c9h	; contents of missing ROM sections

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

	org	0

RST0:	mvi	a,0c9h		;; 0000: 3e c9       >.
	sta	L1fdc		;; 0002: 32 dc 1f    2..
	jr	L0071		;; 0005: 18 6a       .j

	db	0ffh

; debug trap/breakpoint
RST1:	xthl			;
	dcx	h		; point PC back to RST 1
	xthl			;
	jr	NMI		; the rest is the same as NMI debug break

	db	0ffh,0ffh,0ffh
RST2:	db	0ffh,0ffh,0ffh,0ffh,0ffh,0ffh,0ffh,0ffh
RST3:	db	0ffh,0ffh,0ffh,0ffh,0ffh,0ffh,0ffh,0ffh
RST4:	db	0ffh,0ffh,0ffh,0ffh,0ffh,0ffh,0ffh,0ffh
RST5:	db	0ffh,0ffh,0ffh,0ffh,0ffh,0ffh,0ffh,0ffh
RST6:	db	0ffh,0ffh,0ffh,0ffh,0ffh,0ffh,0ffh,0ffh

RST7:	jmp	L1fde		; user debug entry?

	db	0ffh,0ffh,0ffh,0ffh,0ffh

; program utility routines
L0040:	jmp	L0094		; user program exit?
L0043:	jmp	L01ce		; console input? w/toupper?
L0046:	jmp	L01e1		; console output (A)
L0049:	jmp	L01c4		; PIO-related? CR/LF?
L004c:	jmp	L03df		; PIO-related?

	db	0ffh,0ffh,0ffh,0ffh,0ffh,0ffh,0ffh,0ffh,0ffh,0ffh,0ffh,0ffh
	db	0ffh,0ffh,0ffh,0ffh,0ffh,0ffh,0ffh,0ffh,0ffh,0ffh,0ffh

; NMI - pushbutton - manual program break.
NMI:	push	psw		;; 0066: f5          .
	lda	monflg		;; 0067: 3a fc 1f    :..
	ana	a		;; 006a: a7          .
	jrnz	L0077		; NMI/RST1 in monitor, reset and start over
	pop	psw		;
	jmp	L02a3		; NMI/RST1 in user program, save registers

; power-on/RESET entry
L0071:	lxi	h,usrstk	;; 0071: 21 a0 1f    ...
	shld	savSP		;; 0074: 22 d6 1f    "..
; re-start monitor (incl. NMI without program running)
L0077:	lxi	sp,monstk	;; 0077: 31 c0 1f    1..
	lxi	h,L03b2		; initialize all hardware...
	mvi	b,20		;
	call	L020e		;
	mvi	a,010h		;; SIO Reset Ext/Status Intrs
	out	sioActl		;
	in	sioActl		;
	bit	5,a		; test CTS - 4800/9600
	jrz	L0094		;
	lxi	h,L03da		; alternate baud (4800?)
	mvi	b,2		;
	call	L020e		;
	; fall-through HL is garbage (L03b2++/L03da++)
; main loop for monitor (HL may contain prev. value?)
L0094:	call	L01bc		; prompt?
	lxi	b,1		;; 0097: 01 01 00    ...
	sbcd	monflg		;; 009a: ed 43 fc 1f .C..
	dcr	c		;; 009e: 0d          .
	xra	a		;; 009f: af          .
	sta	numflg		;; 00a0: 32 fb 1f    2..
	call	L01a6		;; 00a3: cd a6 01    ...
	jrc	L00bf		;; 00a6: 38 17       8.
	; entry was a hex digit... reset HL=0 and start accumulating
	lxi	h,0		;; 00a8: 21 00 00    ...
	jr	L00b2		;; 00ab: 18 05       ..

; wait for input (from SIO? keypad? ???)
L00ad:	call	L01a6		;; 00ad: cd a6 01    ...
	jrc	L00bf		; break out if non-hex char, HL=value
L00b2:	inr	a		;; 00b2: 3c          <
	sta	numflg		;; 00b3: 32 fb 1f    2..
	dcr	a		;; 00b6: 3d          =
	dad	h		;; 00b7: 29          )
	dad	h		;; 00b8: 29          )
	dad	h		;; 00b9: 29          )
	dad	h		;; 00ba: 29          )
	add	l		;; 00bb: 85          .
	mov	l,a		;; 00bc: 6f          o
	jr	L00ad		;; 00bd: 18 ee       ..

; got input, parse command char, HL may have value (i.e. postfix)
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
L00f4:	jr	L0094		;; 00f4: 18 9e       ..

; 'P' command - like 'G' but PC+1? For continuing after RST1?
L00f6:	lhld	savPC		;; 00f6: 2a d8 1f    *..
	inx	h		;; 00f9: 23          #
	jr	L0103		;; 00fa: 18 07       ..

; 'G'o command, check for address entered.
L00fc:	lda	numflg		;; 00fc: 3a fb 1f    :..
	ana	a		;; 00ff: a7          .
	jz	L02ea	; resume with whatever is in savPC
	GAP	0103h
L0103:	; 6 bytes
	shld	savPC	; set new savPC for resuming
	jmp	L02ea	; now resume normally

	GAP	0109h
; LF (9 bytes)
L0109:
	ds	7
	jr	L0094

	GAP	0112h
; CR (6 bytes)
L0112:
; wild guess - print byte at (HL++)?
	mov	a,m
L0113:	call	L01ed
	jr	L00f4
; wild guess - print HL?
;	call	L0225
;	jmp	L0094

	GAP	0118h
; '^' command - opposite of 'V'?
L0118:	; 7 bytes
	ds	5
	jr	L00f4	; 7 bytes

	GAP	011fh
; '/' command - print (HL) 16-bit value
; need to end with HL+=2? and print hi byte first?
L011f:	; (8 bytes)
	mov	e,m
	inx	h
	call	L01ec
	mov	a,e
	jr	L0113 ; print byte and ++HL (8 bytes)

	GAP	0127h
; 'T' command (6 bytes)
L0127:
	; wild guess - (T)op-of-stack?
	lhld	savSP
	jmp	L011f

	GAP	012dh
; 'V' command - same as H(^) ? Opposite of '^' (does L0118 jump here)?
L012d:	; (36 bytes)
	ds	33	; TODO
	jmp	L0094

	GAP	0151h
; 'I' command (85 bytes)
L0151:
	; wild guess - (I)nstruction dump?
	ds	82	; TODO
	jmp	L0094

	GAP	01a6h
; input a digit
; returns A=char, CY if not HEX digit
; returns A=value, NC else
; needs echo?
L01a6:	; (22 bytes)
	call	L01ce	; does toupper
	call	L01e1	; echo
	jr	L01fb	; convert to num if valid
L01ae:
	ds	14	; TODO:
; 15 bytes: input 2 HEX digits, return value
; or keep cksum...  22 bytes
;	call	L01ec
;	rc
;	add	a
;	add	a
;	add	a
;	add	a
;;	push	b
;	mov	b,a
;	call	L01ec
;;	jrc	xxx
;	ora	b
;;	mov	b,a
;;	add	l
;;	mov	l,a
;;	mov	a,b
;;xxx:	pop	b
;	ret

	GAP	01bch
; prompt? 8 bytes
L01bc:
	call	L0049
	mvi	a,'*'	; TODO: what is prompt char?
	jmp	L0046

	GAP	01c4h

; CR/LF? 10 bytes
L01c4:
	mvi	a,CR
	call	L0046
	mvi	a,LF
	jmp	L0046

	GAP	01ceh
; console input? 19 bytes (w/toupper?) (no echo)
L01ce:
	in	sioActl
	bit	0,a		; Rx Available
	jrz	L01ce
	in	sioAdat
	ani	7fh	;?
	cpi	'a'
	rc
	cpi	'z'+1
	rnc
	ani	5fh
	ret	; 19 bytes

	GAP	01e1h
; 31 bytes total...
; console output to Ch A
L01e1:
	push	psw
L01e2:
	in	sioActl
	bit	2,a		; Tx Empty
	jrz	L01e2
	pop	psw
	out	sioAdat
	ret	; 11 bytes

 if 0
; who calls this? HEX load?
L01ec:
	call	L05e3	; input chB?
	ori	0	; TODO - 2 bytes
 else
L01ec:	mov	a,m
L01ed:	inx	h
	jmp	L022a
 endif
; ... convert ASCII to HEX - return CY if not HEX digit
;	toupper?
L01f1:	; toupper, hex2int, CY if not HEX digit else value
	cpi	'a'
	jrc	L01fb
	cpi	'z'+1
	jrnc	L01fb
	ani	5fh
L01fb:	; hex2int, CY if not HEX digit else value
	cpi	'0'
	rc
	cpi	'F'+1
	GAP	0200h
	cmc			;; 0200: 3f          ?
	rc			;; 0201: d8          .
	cpi	'9'+1		;; 0202: fe 3a       .:
	jrc	L020b		;; 0204: 38 05       8.
	cpi	'A'		;; 0206: fe 41       .A
	rc			;; 0208: d8          .
	sui	'A'-'9'-1	;; 0209: d6 07       ..
L020b:	sui	'0'		;; 020b: d6 30       .0
	ret			;; 020d: c9          .

; I/O init: process 'B' pairs of port/data at HL
L020e:	mov	c,m		;; 020e: 4e          N
	inx	h		;; 020f: 23          #
	outi			;; 0210: ed a3       ..
	jrnz	L020e		;; 0212: 20 fa        .
	ret			;; 0214: c9          .

; HL = location or usregs addr to print
L0215:	push	b		;; 0215: c5          .
	push	h		;; 0216: e5          .
	lxi	b,-endrgs	;; 0217: 01 26 e0    .&.
	dad	b		;; 021a: 09          .
	jrc	L0223		;; 021b: 38 06       8.
	; below 'endrgs'
	lxi	b,endrgs-usregs	;; 021d: 01 1a 00    ...
	dad	b		;; 0220: 09          .
	jrc	L0247		; must be register
	; below 'usregs'
; print HL in HEX
L0223:	pop	h		;; 0223: e1          .
	pop	b		;; 0224: c1          .
L0225:	mov	a,h		;; 0225: 7c          |
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

; usregs < HL < endrgs
; output register mnemonic corresponding to HL
L0247:	pop	h		;; 0247: e1          .
	push	h		;; 0248: e5          .
	lxi	b,L0399-usregs
	dad	b		;; 024c: 09          .
	mvi	a,'R'		;; 024d: 3e 52       >R
	call	L01e1		;; 024f: cd e1 01    ...
	mov	a,m		;; 0252: 7e          ~
	ana	a		;; 0253: a7          .
	jrnz	L025a		;; 0254: 20 04        .
	mvi	a,'+'		;; 0256: 3e 2b       >+
	jr	L0265		;; 0258: 18 0b       ..

; output register name (H -> "H", h -> "H'")
L025a:	bit	5,a		;; 025a: cb 6f       .o
	jrz	L0265		;; 025c: 28 07       (.
	ani	05fh		; toupper
	call	L01e1		;; 0260: cd e1 01    ...
	mvi	a,027h		;; 0263: 3e 27       >'
L0265:	call	L01e1		;; 0265: cd e1 01    ...
	pop	h		;; 0268: e1          .
	pop	b		;; 0269: c1          .
	ret			;; 026a: c9          .

; 'R' command - <char>[']/ - dump register
L026b:	call	L01ce		; input
	call	L01e1		; echo
	mov	e,a		;
	call	L0297		; validate
	call	L01ce		; input - either ' or /
	call	L01e1		; echo
	cpi	027h		; check '
	jrnz	L028b		;
	mov	a,e		;
	ori	020h		; tolower = alt reg
	call	L0297		; validate
	call	L01ce		; input
	call	L01e1		; echo
L028b:	cpi	'/'		;; 028b: fe 2f       ./
	jnz	L00ef		; error...
	lxi	b,usregs-L0399-1
	dad	b		; HL=register value location
	jmp	L011f		; dump register contents?

L0297:	lxi	h,L0399		;; 0297: 21 99 03    ...
	lxi	b,25		;; 029a: 01 19 00    ...
	ccir			; HL=(match+1)
	rz			;; 029f: c8          .
	jmp	L00ef		;; 02a0: c3 ef 00    ...

; User's program was running... save everything and enter debuger...
L02a3:	shld	savHL		;; 02a3: 22 da 1f    "..
	pop	h		;; 02a6: e1          .
	shld	savPC		;; 02a7: 22 d8 1f    "..
	sspd	savSP		;; 02aa: ed 73 d6 1f .s..
	lxi	sp,dbgstk	;; 02ae: 31 d6 1f    1..
	push	psw		;; 02b1: f5          .
	ldai			; parity set from IFF2 (intr enable)
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
	lhld	savHL		;; 02d2: 2a da 1f    *..
	push	h		;; 02d5: e5          .
	push	d		;; 02d6: d5          .
	push	b		;; 02d7: c5          .
	push	psw		;; 02d8: f5          .
	call	L01c4		; printf("\r\n>%04x",savPC)
	mvi	a,'>'		;; 02dc: 3e 3e       >>
	call	L01e1		;; 02de: cd e1 01    ...
	lhld	savPC		;; 02e1: 2a d8 1f    *..
	call	L0215		;; 02e4: cd 15 02    ...
	jmp	L0094		; enter monitor loop

; Restore registers? "return to program"?
L02ea:	xra	a		;; 02ea: af          .
	sta	monflg		;; 02eb: 32 fc 1f    2..
	pop	psw		;; 02ee: f1          .
	pop	b		;; 02ef: c1          .
	pop	d		;; 02f0: d1          .
	pop	h		;; 02f1: e1          .
	shld	savHL		;; 02f2: 22 da 1f    "..
	exaf			;; 02f5: 08          .
	exx			;; 02f6: d9          .
	pop	psw		;; 02f7: f1          .
	pop	b		;; 02f8: c1          .
	pop	d		;; 02f9: d1          .
	pop	h		;; 02fa: e1          .
	exaf			;; 02fb: 08          .
	exx			;; 02fc: d9          .
	pop	h		; I R
	push	psw		;; 02fe: f5          .
	mov	a,h		;; 02ff: 7c          |
	; lost code... 43 bytes
	stai
	mov	a,l
	star
	pop	psw
	popix
	popiy
	lspd	savSP
	; ??? put savPC on stack?
	push	h
	lhld	savPC
	xthl
	jmp	L1fdc	; resume program
	; 22 bytes
L0316:
	ds	21	; subroutine for L032b?

	GAP	032bh
; Intel HEX load from ChB? (110 bytes) incl. POP HL,DE,BC,PSW; RET
L032b:
	; C=sioBctl?
	ds	105	; TODO
	pop	h
	pop	d
	pop	b
	pop	psw
	ret

	GAP	0399h
L0399:	;ds	25	; table for ccir at L0297 (register mnemonic chars)
; something like this? - matches order of registers stored in 'usregs'.
	db	'A',0	; AF
	db	'B',0	; BC
	db	'D',0	; DE
	db	'H',0	; HL
	db	'a',0	; AF' - "A'"
	db	'b',0	; BC' - "B'"
	db	'd',0	; DE' - "D'"
	db	'h',0	; HL' - "H'"
	db	'I',0	; I R
	db	'X',0	; IX
	db	'Y',0	; IY
	db	'S',0	; SP
	db	'P'	; PC

; 110 baud:	64x, 177 count
; 300 baud:	32x, 130 count
; 1200 baud:	16x, 65 count
; 4800 baud:	16x, 16 count
; 9600 baud:	16x, 8 count

L03b2:	;ds	2*20	; I/O init table
	; (TODO: CTC ch0 and/or ch1 at 9600?)
	db	ctc0,045h	; Ch0: COUNTER, TC follows
	db	ctc0,008h	; Ch0: 1.25MHz / 8 / 16x = 9765.625 (9600+1.7%)
	db	ctc1,045h	; Ch1: COUNTER, TC follows
	db	ctc1,008h	; TODO: what baud for ChB?
	; (TODO: SIO ch reset?)
 if 1
	db	sioActl,018h	; ChA RESET
 endif
	db	sioActl,004h	; ChA: WR4
	db	sioActl,044h	; ChA: 16x, 1 stop, NP
	db	sioActl,005h	; ChA: WR5
	db	sioActl,068h	; ChA: Tx 8-bit, enable
	db	sioActl,003h	; ChA: WR3
	db	sioActl,0c1h	; ChA: Rx 8-bit, enable
 if 1
	db	sioBctl,018h	; ChB RESET
 endif
	db	sioBctl,004h	; ChB: WR4
	db	sioBctl,044h	; ChB: 16x, 1 stop, NP - or 32x/64x for lower bauds?
	db	sioBctl,005h	; ChB: WR5
	db	sioBctl,068h	; ChB: Tx 8-bit, enable
	db	sioBctl,003h	; ChB: WR3
	db	sioBctl,0c1h	; ChB: Rx 8-bit, enable
	; TODO: are these used for PIO or something else?
	; like 2x SIO CH RESET and PIO2 ChB control mode?
 if 1
	db	pio2Bc,0cfh	; PIO2 ChB: control mode
	db	pio2Bc,00fh	; PIO2 ChB: O O O O I I I I
 else
	db	pio1Ac,04fh	; PIO1 ChA: input mode
	db	pio1Bc,04fh	; PIO1 ChB: input mode
	db	pio2Ac,04fh	; PIO2 ChA: input mode
	db	pio2Bc,04fh	; PIO2 ChB: input mode
 endif

L03da:	;ds	2*2	; alt. baud
	; TODO: ch0 or ch1? 4800?
	db	ctc0,045h	; Ch0: COUNTER, TC follows
	db	ctc0,010h	; Ch0: 1.25MHz / 16 / 16x = 4882.8125 (4800+1.7%)

	;ds	1		;; 03de: c9          .
	db	GONE

; missing routine for vector L004c (an H(?) command) (e.g. A=2)
L03df:	; 7 bytes
	ds	6	; TODO
	ret	; or jmp...ret

	GAP	03e6h
; loop back, check sioA and sioB for input.
; ... after user types char or HEX line processed
; also, initial entry? where called from?
L03e6:	; 5 bytes
	; wild guess
	mvi	a,NULL
	sta	NULL

	GAP	03ebh
; loop back, check sioA and sioB for input.
; jump to L0408 if sioA input not ready
; TODO: somewhere something does "setx 1,+0" for HEX file loading
; Also, "lxix" or "lixd" is needed...
L03eb:	; 21 bytes
	; check ChA for input
	in	sioActl
	bit	0,a		; Rx Available
	jrz	L0408
	in	sioBctl
	bit	2,a		; Tx Empty
	jrz	L0408
	in	sioBdat
	ani	7fh
	cpi	'X'	; or ?
	jz	L0094	; ????
	GAP	0400h
	; processing char from sioAdat?
	cpi	01dh		; ^] = "send break"?
	jrz	L043c		; if BREAK requested
	out	sioBdat		; pass-thru char A->B
	jr	L03e6		;; 0406: 18 de       ..

L0408:
	; check ChB for input
	in	sioBctl		;; 0408: db 07       ..
	bit	0,a		; Rx Available
	jrz	L03eb		;; 040c: 28 dd       (.
	in	sioActl		;; 040e: db 06       ..
	bit	2,a		; Tx Empty
	jrz	L03eb		;; 0412: 28 d7       (.
	in	sioBdat		;; 0414: db 05       ..
	bitx	1,+0		; HEX file?
	jrz	L0438		; no, pass-thru
	bitx	7,+0		; between lines?
	jrz	L0426		; no, check EOL
	cpi	':'		; start of HEX?
	jrz	L045e		; yes, process it
L0426:	cpi	LF		;; 0426: fe 0a       ..
	jrz	L0434		;; 0428: 28 0a       (.
	cpi	CR		;; 042a: fe 0d       ..
	jrz	L0434		;; 042c: 28 06       (.
	resx	7,+0		; not EOL, clear flag
	jr	L0438		;; 0432: 18 04       ..

L0434:	setx	7,+0		; EOL, set "between lines" flag
L0438:	out	sioAdat		; pass-thru char
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

; ':' seen on ch B - process HEX data?
L045e:	call	L0463		;; 045e: cd 63 04    .c.
	jr	L03e6		;; 0461: 18 83       ..

L0463:	push	psw		;; 0463: f5          .
	push	b		;; 0464: c5          .
	push	d		;; 0465: d5          .
	push	h		;; 0466: e5          .
	mvi	c,007h		; sioBctl?
	xra	a		;; 0469: af          .
	jmp	L032b		;; 046a: c3 2b 03    .+.

				; 046d: ff ...
	db	0ffh,0ffh,0ffh,0ffh,0ffh,0ffh,0ffh,0ffh
	db	0ffh,0ffh,0ffh,0ffh,0ffh,0ffh,0ffh,0ffh
	db	0ffh,0ffh,0ffh	; ... 047f: ff          .

; 'H' command - input octal digits... setup PIO for output?
L0480:	mvi	a,00fh		; PIO output mode
	out	pio1Ac		;; 0482: d3 0a       ..
	out	pio1Bc		;; 0484: d3 0b       ..
	out	pio2Ac		;; 0486: d3 0e       ..
	mvi	a,0cfh		; PIO control mode
	out	pio2Bc		;; 048a: d3 0f       ..
	mvi	a,00fh		; O O O O I I I I
	out	pio2Bc		;; 048e: d3 0f       ..
	lxiy	L07d6		;; 0490: fd 21 d6 07 ....
; input octal number, command char...
L0494:	call	L0049		; CR/LF?
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
	call	L0046		; echo to console
	sui	'0'		;; 04b1: d6 30       .0
	dad	h		;; 04b3: 29          )
	dad	h		;; 04b4: 29          )
	dad	h		;; 04b5: 29          )
	add	l		;; 04b6: 85          .
	mov	l,a		;; 04b7: 6f          o
	setb	0,c		;; 04b8: cb c1       ..
	jr	L04a3		;; 04ba: 18 e7       ..

; C=1 if number was entered, HL=number
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
	call	L0046		; echo to console
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
	jz	L0606		; "get" from PIO device
	cpi	'P'		;; 04fc: fe 50       .P
	jz	L062b		; "put" to PIO device
	; 10 bytes... 2x(CPI n; JMP mmmm)?
	; L0653
	; L065b
	; L068b
	ds	7	; TODO
	jmp	L0494

	GAP	050bh
; H(?) command (L065b) - (HL & 0x0fff) < 4...
; or error?
L050b:	; 7 bytes
	mvi	a,'!'
	call	L0046
	jr	L0494

	GAP	0512h
; H(S) command
L0512:	; 5 bytes
	ds	2	; TODO
	jmp	L0494 ; or L05xx?

	GAP	0517h
; H(R) command
L0517:	; 12 bytes
	ds	9	; TODO
	jmp	L0494 ; or L05xx?

	GAP	0523h
; H(CR) command
L0523:	; 8 bytes
	ds	5	; TODO
	jmp	L0494 ; or L05xx?

	GAP	052bh
; H(.) command
L052b:	; 5 bytes
	ds	2	; TODO
	jmp	L0494 ; or L05xx?

	GAP	0530h
; H(>) command
L0530:	; 8 bytes
	ds	5	; TODO
	jmp	L0494

	GAP	0538h
; H(,) command
L0538:	; 5 bytes
	ds	2	; TODO
	jmp	L0494

	GAP	053dh
; H(<) command
L053d:	; 11 bytes
	ds	8	; TODO
	jmp	L0494

	GAP	0548h
; H(LF) command
L0548:	; 23 bytes
	ds	20	; TODO
	jmp	L0494

	GAP	055fh
; H(\) command (5 bytes)
L055f:
	ds	2	; TODO
	jmp	L0494

	GAP	0564h
; H(^) command
L0564:	; 36 bytes
	ds	33	; TODO
	jmp	L0494

	GAP	0588h
; H(/) command
L0588:	; 55 bytes
	ds	52	; TODO
	jmp	L0494

	GAP	05bfh
; prepare PIOs (or device) for bulk transfer? (36 bytes)
; used by H(G) and H(P) commands. Also L065b (H(?)), L068b (H(M))
L05bf:
	ds	35	; TODO
	ret

	GAP	05e3h
; For H(*) commands, input char/key from ??? Chan B? (11 bytes)
L05e3:
	in	sioBctl
	bit	0,a		; Rx Available
	jrz	L05e3
	in	sioBdat
	ani	7fh	;?
	ret	; 11 bytes

	GAP	05eeh
; prepare to read/write from PIO device? (18 bytes)
; pio2Bd (bits 0-3) has data on return, or pio2Ad is ready to take data.
; possibly "strobe" the external device, or otherwise cause it to enable
; data output.
L05ee:
	push	psw
	push	b
	;... 16/17 bytes
	ds	16	; TODO
	GAP	0600h
	; TODO: is this "rlc" or stray operand byte?
	rlc			;; 0600: 07          .
	out	pio1Ad		;; 0601: d3 08       ..
	pop	b		;; 0603: c1          .
	pop	psw		;; 0604: f1          .
	ret			;; 0605: c9          .

; H(G) - input 2K bytes as nibbles from PIO2B into 1600-1DFF
L0606:	call	L05bf		;; 0606: cd bf 05    ...
	lxi	d,0		;; 0609: 11 00 00    ...
	lxi	h,piobuf	;; 060c: 21 00 16    ...
	lxi	b,-2048		;; 060f: 01 00 f8    ...
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
	jmp	L0494		; get next sub-cmd

; H(P) - send 2K bytes as nibbles out PIO2A from 1600-1DFF.
L062b:	call	L05bf		;; 062b: cd bf 05    ...
	lxi	d,0		;; 062e: 11 00 00    ...
	lxi	h,piobuf	;; 0631: 21 00 16    ...
	lxi	b,-2048		;; 0634: 01 00 f8    ...
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
	jmp	L0494		; get next sub-cmd

; H(?) command
L0653:	mvi	a,002h		;; 0653: 3e 02       >.
	call	L004c		;; 0655: cd 4c 00    .L.
	jmp	L0494		; get next sub-cmd

; H(?) command - HL=?
L065b:	call	L05bf		;; 065b: cd bf 05    ...
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
	jmp	L0494		; get next sub-cmd

; H(?) command
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
	; missing 178+1 bytes
	ds	178	; TODO

	GAP	07b3h
; output/save/dump 1 byte of PIO data? prefix with space? (21 bytes)
L07b3:
	push	psw
	mvi	a,' '
	call	L0046
	pop	psw
	jmp	L022a
	; 11 more bytes??? ChB output?
	ds	11	; TODO

	GAP	07c8h
; term/separator for PIO data output L07b3? (14 bytes)
L07c8:
	ds	14	; TODO

 	GAP	07d6h
; status/context area for PIO transfers (unknown length)
L07d6:	db	GONE			;; 07d6: c9          .
; code? or?
	rept	0800h-$
	db	GONE
	endm
 if $ <> 0800h
	.error	'ROM overflow'
 endif

	; RAM starts at 1000h
	org	1600h
piobuf:	ds	2048	; buffer for PIO device transfers
; L1e00:
	ds	416	; all user stack?
usrstk:	ds	0

	ds	32	; monitor stack
monstk:	ds	0
usregs:	; user registers during debug, cont. in monstk
	; This area is matched with 'L0399'
	ds	2	; AF
	ds	2	; BC
	ds	2	; DE
	ds	2	; HL
	ds	2	; AF'
	ds	2	; BC'
	ds	2	; DE'
	ds	2	; HL'
	ds	2	; I R
	ds	2	; IX
	ds	2	; IY
dbgstk:	; used to push registers
savSP:	ds	2	; SP
savPC:	ds	2	; PC
endrgs:	; end of debug register storage

savHL:	ds	2	; saved HL
L1fdc:	ds	2	; return-to-user code (RET; NOP or EI; RET)
L1fde:	ds	29	; RST7 handler (not initialized by ROM?)
numflg:	ds	1	; non-zero (digit+1) if number preceded command character
monflg:	ds	1	; flag indicating running in monitor (1)
	ds	1
L1ffe:	ds	2

	end
