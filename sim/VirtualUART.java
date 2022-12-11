// Copyright (c) 2017 Douglas Miller <durgadas311@gmail.com>

public interface VirtualUART {
	static final int SET_CTS = 0x01; // Settable and Readable
	static final int SET_DSR = 0x02; // Settable and Readable
	static final int SET_DCD = 0x04; // Settable and Readable
	static final int SET_RI  = 0x08; // Settable and Readable
	static final int GET_RTS = 0x10; // Readable
	static final int GET_DTR = 0x20; // Readable
	static final int GET_OT1 = 0x40; // Readable
	static final int GET_OT2 = 0x80; // Readable
	static final int GET_BREAK = 0x100; // Readable
	static final int GET_ONLY = (GET_RTS | GET_DTR | GET_OT1 |
			GET_OT2 | GET_BREAK);
	static final int GET_CHR = 0x8000; // flags output as modem ctrl chg
	int available();	// Num bytes available from UART Tx.
	int take();		// Get byte from UART Tx, possibly sleep.
	boolean ready();	// Can UART Rx accept byte without overrun?
	void put(int ch, boolean sleep);	// Put byte into UART Rx.
	void setModem(int mdm);	// Change Modem Control Lines in to UART.
	int getModem();		// Get all Modem Control Lines for UART.
	boolean attach(Object periph);
	void detach();		// Peripheral no longer usable
	String getPortId();	// for properties
	void attachDevice(SerialDevice io);	//
}
