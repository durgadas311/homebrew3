// Copyright (c) 2018 Douglas Miller <durgadas311@gmail.com>

import java.util.Arrays;
import java.util.Vector;
import java.util.Properties;
import java.io.*;
import java.awt.Font;

public class StdioSerial implements Runnable {
	VirtualUART uart;
	output input;

	public StdioSerial(Properties props, Vector<String> argv, VirtualUART uart) {
		this.uart = uart;
		Thread t = new Thread(this);
		t.start();
		input = new output();
		// TODO: allow special program codes to change?
		uart.setModem(VirtualUART.SET_CTS | VirtualUART.SET_DSR);
	}

	public class output implements Runnable {
		public output() {
			Thread t = new Thread(this);
			t.start();
		}
		public void run() {
			while (true) {
				try {
					int c = uart.take();
					System.out.write(c);
					System.out.flush();
				} catch (Exception ee) {
					ee.printStackTrace();
					uart.detach(); // repetative?
					break;
				}
			}
		}
	}

	// This thread reads program stdout and sends to UART
	public void run() {
		while (true) {
			try {
				// This probably needs to be throttled...
				int c = System.in.read();
				if (c == '\n') c = '\r';
				if (c == '\001') c = '\003';
				if (c == '\177') c = '\010';
				// Wait for RTS...
				while ((uart.getModem() & VirtualUART.GET_RTS) == 0);
				while (!uart.ready());
				// Overrun UART, if that's what user wants...
if (!uart.ready()) {
System.err.format("Overrun %02x\n", c);
}
				uart.put(c, false);
			} catch (Exception ee) {
				ee.printStackTrace();
				uart.detach(); // repetative?
				break;
			}
		}
	}
}
