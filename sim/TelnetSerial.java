// Copyright (c) 2017 Douglas Miller <durgadas311@gmail.com>

import java.util.Arrays;
import java.util.Vector;
import java.util.Properties;
import java.io.*;
import java.net.*;

public class TelnetSerial implements SerialDevice, Runnable {
	VirtualUART uart;
	ServerSocket listen;
	Socket conn;
	String remote;
	int remPort;
	String dbg;
	InputStream inp;
	OutputStream out;
	boolean modem = false;
	boolean nodtr = false;

	public TelnetSerial(Properties props, Vector<String> argv, VirtualUART uart) {
		this.uart = uart;
		if (argv.size() < 3 || argv.size() > 5) {
			System.err.format("TelnetSerial: Invalid args\n");
			return;
		}
		if (argv.size() > 3) {
			for (int x = 3; x < argv.size(); ++x) {
				if (argv.get(x).equalsIgnoreCase("modem")) {
					modem = true;
				} else if (argv.get(x).equalsIgnoreCase("nodtr")) {
					nodtr = true;
				}
			}
		}
		String host = argv.get(1);
		int port = Integer.valueOf(argv.get(2));
		dbg = String.format("TelnetSerial %s %d\n", host, port);
		InetAddress ia;
		try {
			if (host.length() == 0 || host.equals("localhost")) {
				ia = InetAddress.getLocalHost();
			} else {
				ia = InetAddress.getByName(host);
			}
			listen = new ServerSocket(port, 1, ia);
		} catch (Exception ee) {
			ee.printStackTrace();
			return;
		}
		uart.attachDevice(this);
		if (!modem) {
			uart.setModem(VirtualUART.SET_CTS |
					VirtualUART.SET_DSR |
					VirtualUART.SET_DCD);
		}
		Thread t = new Thread(this);
		t.start();
	}

	// SerialDevice interface:
	//
	public void write(int b) {
		if (conn == null) {
			return;
		}
		try {
			out.write(b);
		} catch (Exception ee) {
			//ee.printStackTrace();
			discon();
		}
	}

	// This should not be used...
	// We push received data from the thread...
	public int read() {
		if (conn == null) {
			return -1;
		}
		// prevent blocking? needed?
		try {
			if (inp.available() < 1) return 0;
			return inp.read();
		} catch (Exception ee) {
			return -1;
		}
	}

	// Not used...
	public int available() {
		if (conn == null) {
			return 0;
		}
		try {
			return inp.available();
		} catch (Exception ee) {
			return -1;
		}
	}

	public void rewind() {}

	// This must NOT call uart.setModem() (or me...)
	public void modemChange(VirtualUART me, int mdm) {
		if (modem && !nodtr && (mdm & VirtualUART.GET_DTR) == 0) {
			discon();
		}
	}
	public int dir() { return SerialDevice.DIR_OUT; }

	public String dumpDebug() {
		String ret = dbg;
		if (conn != null) {
			ret += String.format("Connected: %s %d\n", remote, remPort);
		} else if (listen != null) {
			ret += "Listening...\n";
		} else {
			ret += "DEAD.\n";
		}
		return ret;
	}
	/////////////////////////////

	private void discon() {
		if (conn == null) {
			return;
		}
		try {
			conn.close();
		} catch (Exception ee) {}
		conn = null;
		if (modem) {
			uart.setModem(0);
		}
	}

	// Should not get here unless conn == null...
	private void tryConn(Socket nc) {
		if (conn != null || (modem && !nodtr &&
			(uart.getModem() & VirtualUART.GET_DTR) == 0)) {
			try {
				nc.close();
			} catch (Exception ee) {}
			return;
		}
		conn = nc;
		try {
			InetAddress ia = conn.getInetAddress();
			remote = ia.getCanonicalHostName();
			remPort = conn.getLocalPort();
			inp = conn.getInputStream();
			out = conn.getOutputStream();
		} catch (Exception ee) {
			conn = null; // try to close()?
			return;
		}
		if (modem) {
			uart.setModem(VirtualUART.SET_CTS |
					VirtualUART.SET_DSR |
					VirtualUART.SET_DCD);
		}
	}

	private boolean _debug = false;
	private int _state = -1;	// 0="normal" (not in telnet protocol)
	static private final int IAC = 255;
	static private final int WILL = 251;
	static private final int WONT = 252;
	static private final int DO = 253;
	static private final int DONT = 254;
	static private final int SB = 250;
	//static private final int SE = 240;
	static private final int EOR = 239;
	private boolean doTelnet(int b) {
		if (b < 0) return false;
		switch (_state) {
		case IAC:
			if (b == IAC) {
				break;
			} else if (b == EOR) {
				if (_debug) System.err.format("EOR\n", b);
				b = 0;
			}
			_state = b;
			break;
		case WILL:
			if (_debug) System.err.format("IAC WILL %d\n", b);
			_state = 0;
			break;
		case WONT:
			if (_debug) System.err.format("IAC WONT %d\n", b);
			_state = 0;
			break;
		case DO:
			if (_debug) System.err.format("IAC DO %d\n", b);
			_state = 0;
			break;
		case DONT:
			if (_debug) System.err.format("IAC DONT %d\n", b);
			_state = 0;
			break;
		case SB:
			if (_debug) System.err.format("IAC DONT %d\n", b);
			_state = 0;
			break;
		case 0: // not in Telnet protocol...
		case -1: // not yet in Telnet protocol...
			if (b != IAC) {
				return false;
			}
			_state = IAC;
			break;
		default:
			System.err.format("IAC %d ???\n", _state);
			_state = 0; // not the right reaction...
			break;
		}
		return true;
	}

	// This thread reads socket and sends to UART
	// When disconnected, just quit and go back to listening mode...
	public void run() {
		Socket rem;
		int c;
		while (listen != null) {
			while (conn != null) {
				try {
					c = inp.read();
					if (c < 0) {
						discon();
						break;
					}
					// Strip off NULs added by telnet...
					if (!doTelnet(c) && c != 0) {
						uart.put(c, false);
					}
				} catch (Exception ee) {
					discon();
					break;
				}
			}
			try {
				rem = listen.accept();
				tryConn(rem);
			} catch (Exception ee) {
				ee.printStackTrace();
				listen = null;
			}
		}
		uart.attachDevice(null);
		uart.detach();
	}
}
