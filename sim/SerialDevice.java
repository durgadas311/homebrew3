// Copyright (c) 2018 Douglas Miller <durgadas311@gmail.com>

// An "active", "push", interface between a serial port and attached device.
// In this interface, the VirtualUART calls the SerialDevice whenever
// the computer initiates an action. The SerialDevice must *NOT* sleep
// or even consume much time in these routines.
//
public interface SerialDevice {
	static final int DIR_IN = 1;
	static final int DIR_OUT = 2;
	static final int DIR_BIDI = (DIR_IN|DIR_OUT);

	void write(int b);	// CPU is writing the serial data port
	int read();		// CPU is reading the serial data port
	int available();	// returns number available bytes on Rx (0/1)
	void rewind();		// If device supports it, restart stream
				// (e.g. rewind cassette tape)
	// bits a la VirtualUART get/setModem()
	void modemChange(VirtualUART me, int mdm);
	int dir();	// DIR_*
	String dumpDebug();
}
