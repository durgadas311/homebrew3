// Copyright (c) 2017 Douglas Miller <durgadas311@gmail.com>

import java.util.Arrays;
import java.util.Vector;
import java.util.Properties;
import java.io.*;
import java.awt.Font;

public class ProgramSerial extends InputStream implements Runnable {
	VirtualUART uart;
	RunProgram prog;

	public ProgramSerial(Properties props, Vector<String> argv, VirtualUART uart) {
		this.uart = uart;
		// WARNING! destructive to caller's 'argv'!
		argv.removeElementAt(0);
		prog = new RunProgram(argv, this, true);
		if (prog.excp == null) {
			Thread t = new Thread(this);
			t.start();
			// TODO: allow special program codes to change?
			// TODO: this computer uses modem control differently...
			//uart.setModem(VirtualUART.SET_CTS | VirtualUART.SET_DSR);
		} else {
			prog.excp.printStackTrace();
		}
	}

	private void doModem(int mdm) {
		// For test purposes:
		System.err.format("MODEM LINES %04x\n", mdm);
	}

	public int read() {
		while (true) {
			int c = uart.take();
			if (c < 0) { // EOF
				uart.detach(); // will close() do this?
				return c;
			}
			if ((c & VirtualUART.GET_CHR) == 0) {
				return c;
			}
			doModem(c);
		}
	}
	public int available() {
		return uart.available();
	}
	public void close() {
		uart.detach();
	}

	// This thread reads program stdout and sends to UART
	public void run() {
		while (true) {
			try {
				// This probably needs to be throttled...
				int c = prog.proc.getInputStream().read();
				if (c < 0) { // EOF
					// program is dead, stop feeding uart...
					break;
				}
				uart.put(c, true);
			} catch (Exception ee) {
				ee.printStackTrace();
				uart.detach(); // repetative?
				break;
			}
		}
	}
}
