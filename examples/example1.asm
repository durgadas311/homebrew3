; Example to display all standard (ASCII 7) characters built-in
; to the HDSP-2111.
;
; * Start simulator with "gui-telnet.rc" config file.
; * Connect to console (e.g. "telnet <host> 31123").
; * Use 'H' and 'T' commands to enter terminal mode.
; * Login to system and 'cat' ('type'...) "example1.hex".
; * Ctrl-^ and 'X' to get back to monitor.
; * Type "1000G" to start program.
;
; Pressing the NMI button will stop the program and
; re-enter the monitor. The program can be resumed with
; the "G" command.
;
	maclib	z80
	org 1000h

; initialize buffer
loop0:	lxi	h,buf
	mvi	b,8
	xra	a
loop1:	mov	m,a
	inr	a
	inx	h
	djnz	loop1

; display buffer
loop5:	lxi	h,buf
	mvi	b,8
	mvi	c,0b8h
loop2:	mov	a,m
	inx	h
	outp	a
	inr	c
	djnz	loop2

; delay some
	mvi	b,2
	lxi	h,0
loop3:	dcx	h
	mov	a,h
	ora	l
	jrnz	loop3
	djnz	loop3

; bump chars to next set
	lxi	h,buf
	mvi	b,8
loop4:	mov	a,m
	adi	8
	ani	7fh
	mov	m,a
	inx	h
	djnz	loop4
	jr	loop5

buf:	ds	8
	end
