// Copyright (c) 2017 Douglas Miller <durgadas311@gmail.com>

import java.util.Arrays;
import java.util.Vector;
import java.util.Properties;
import java.io.*;
import java.awt.Font;

public class ProgramParallel extends InputStream implements Runnable {
	VirtualPPort pport;
	RunProgram prog;

	public ProgramParallel(Properties props, Vector<String> argv, VirtualPPort pport) {
		this.pport = pport;
		// WARNING! destructive to caller's 'argv'!
		argv.removeElementAt(0);
		prog = new RunProgram(argv, this, true);
		if (prog.excp == null) {
			Thread t = new Thread(this);
			t.start();
		} else {
			prog.excp.printStackTrace();
		}
	}

	public int read() {
		int c = pport.take(true);
		if (c < 0) { // EOF
			pport.detach(); // will close() do this?
		}
		return c;
	}
	public int available() {
		return pport.available();
	}
	public void close() {
		pport.detach();
	}

	// This thread reads program stdout and sends to PPort.
	// If the port does not accept input, it should spend
	// all it's time sleeping in read().
	public void run() {
		while (true) {
			try {
				// This probably needs to be throttled...
				int c = prog.proc.getInputStream().read();
				pport.put(c, true);
			} catch (Exception ee) {
				ee.printStackTrace();
				pport.detach(); // repetative?
				break;
			}
		}
	}
}
