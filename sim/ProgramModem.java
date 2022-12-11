// Copyright (c) 2017 Douglas Miller <durgadas311@gmail.com>

import java.util.Arrays;
import java.util.Vector;
import java.util.Properties;
import java.io.*;
import java.awt.Font;

public class ProgramModem extends InputStream implements Runnable {
	VirtualUART uart;
	RunProgram prog;

	public ProgramModem(Properties props, Vector<String> argv, VirtualUART uart) {
		this.uart = uart;
		// WARNING! destructive to 'argv'!
		argv.removeElementAt(0);
		prog = new RunProgram(argv, this);
		// Start program later, by modem control
	}

	// TODO: work out exactly how to do this...
	private void start() {
		if (prog.excp == null) {
			Thread t = new Thread(this);
			t.start();
			prog.excp = null;
			// TODO: allow special program codes to change?
			uart.setModem(VirtualUART.SET_CTS | VirtualUART.SET_DSR);
		} else {
			prog.excp.printStackTrace();
			uart.detach(); // repetative?
		}
	}

	private void stop() {
		if (prog.excp == null) {
			prog.proc.destroy();
			try {
				prog.proc.waitFor();
			} catch (Exception ee) {}
			prog.excp = new IllegalThreadStateException("Terminated");
		} else {
			prog.excp.printStackTrace();
			uart.detach(); // repetative?
		}
	}

	private void doModem(int mdm) {
System.err.format("MODEM LINES %04x\n", mdm);
		// if ... start();
		// else stop();
	}

	public int read() {
		while (true) {
			int c = uart.take();
			if (c < 0) { // EOF
				uart.detach(); // repetative?
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
				uart.put(c, true);
			} catch (Exception ee) {
				ee.printStackTrace();
				uart.detach();
				break;
			}
		}
	}
}
