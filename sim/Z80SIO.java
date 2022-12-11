// Copyright (c) 2017 Douglas Miller <durgadas311@gmail.com>

import java.util.Arrays;
import java.util.Vector;
import java.util.Properties;
import java.io.*;
import java.util.concurrent.Semaphore;
import java.lang.reflect.Constructor;

public class Z80SIO implements IODevice, InterruptController {
	static final int fifoLimit = 10; // should never even exceed 2
	private Interruptor intr;
	private int src;
	private int basePort;
	private String name = null;

	private Z80SIOPort[] ports = new Z80SIOPort[2];
	private int intrs = 0;

	public Z80SIO(Properties props, String pfxA, String pfxB, int base,
			Interruptor intr) {
		name = String.format("Z80SIO%d", (base >> 3) + 1);
		this.intr = intr;
		src = intr.registerINT(0);
		intr.addIntrController(this);
		basePort = base;
		ports[0] = new Z80SIOPort(props, pfxA, 0, null);
		ports[1] = new Z80SIOPort(props, pfxB, 1, ports[0]);
		reset();
	}

	///////////////////////////////
	/// Interfaces for IODevice ///
	public int in(int port) {
		int x = port & 1;
		return ports[x].in(port >> 1);
	}

	public void out(int port, int val) {
		int x = port & 1;
		ports[x].out(port >> 1, val);
	}

	public void reset() {
		intrs = 0;
		intr.lowerINT(0, src);
		ports[0].reset();
		ports[1].reset();
	}
	public int getBaseAddress() {
		return basePort;
	}
	public int getNumPorts() {
		return 4;
	}
	public String getDeviceName() {
		return name;
	}

	private void raiseINT(int idx) {
		int i = intrs;
		intrs |= (1 << idx);
		if (i == 0) {
			intr.raiseINT(0, src);
		}
	}

	private void lowerINT(int idx) {
		int i = intrs;
		intrs &= ~(1 << idx);
		if (i != 0 && intrs == 0) {
			intr.lowerINT(0, src);
		}
	}

	public VirtualUART portA() { return ports[0]; }
	public BaudListener clockA() { return ports[0]; }
	public VirtualUART portB() { return ports[1]; }
	public BaudListener clockB() { return ports[1]; }

	public int readDataBus() {
		if (intrs == 0) {
			return -1;
		}
		int vec = -1;
		// Only Ch B has the vector, but either may interrupt...
		vec = ports[1].readDataBus();
		return vec;
	}

	public boolean retIntr() {
		if (intrs == 0) {
			return false;
		}
		if ((intrs & 1) != 0) {
			ports[0].retIntr();
		} else if ((intrs & 2) != 0) {
			ports[1].retIntr();
		}
		if (intrs == 0) {
			intr.lowerINT(0, src);
		}
		return true;
	}

	class Z80SIOPort implements VirtualUART, BaudListener {
		private java.util.concurrent.LinkedBlockingDeque<Integer> fifo;
		private java.util.concurrent.LinkedBlockingDeque<Integer> fifi;
		private byte[] wr;
		private byte[] rr;

		static final int rr0_rxr_c = 0x01;
		static final int rr0_int_c = 0x02;
		static final int rr0_txp_c = 0x04;
		static final int rr0_dcd_c = 0x08;
		static final int rr0_syn_c = 0x10;
		static final int rr0_cts_c = 0x20;
		static final int rr0_txu_c = 0x40;
		static final int rr0_brk_c = 0x80;

		static final int wr5_dtr_c = 0x80;
		static final int wr5_brk_c = 0x10;
		static final int wr5_rts_c = 0x02;

		private Object attObj;
		private OutputStream attFile;
		private boolean excl = true;
		private long lastTx = 0;
		private long lastRx = 0;
		private long nanoBaud = 0; // length of char in nanoseconds
		private int bits; // bits per character
		private int index;
		private Z80SIOPort chA; // null on Ch A
		private int intrs;
		private int mdms = 0;	// modem inputs, floating
		private boolean rr0Latch = false;
		private int modem = -1;
		private Semaphore wait;

		private SerialDevice io = null;
		private boolean io_in;
		private boolean io_out;

		public Z80SIOPort(Properties props, String pfx, int idx, Z80SIOPort alt) {
			chA = alt;
			attObj = null;
			attFile = null;
			index = idx;
			wait = new Semaphore(0);
			fifo = new java.util.concurrent.LinkedBlockingDeque<Integer>();
			fifi = new java.util.concurrent.LinkedBlockingDeque<Integer>();
			wr = new byte[8];
			rr = new byte[8]; // only 3 useable...
			String s = props.getProperty(pfx + "_att");
			if (s != null && s.length() > 1) {
				if (s.charAt(0) == '>') { // redirect output to file
					attachFile(s.substring(1));
				} else if (s.charAt(0) == '!') { // pipe to/from program
					attachPipe(s.substring(1));
				} else {
					attachClass(props, s);
				}
			}
		}

		private void attachFile(String s) {
			String[] args = s.split("\\s");
			setupFile(args, 0);
		}

		private void setupFile(String[] args, int start) {
			boolean append = false;
			for (int x = start + 1; x < args.length; ++x) {
				if (args[x].equals("a")) {
					append = true;
				} else if (args[x].equals("+")) {
					excl = false;
				}
			}
			if (args[start].equalsIgnoreCase("syslog")) {
				attFile = System.err;
				excl = false;
			} else try {
				attFile = new FileOutputStream(args[start], append);
				if (excl) {
					setModem(VirtualUART.SET_CTS | VirtualUART.SET_DSR);
				}
			} catch (Exception ee) {
				System.err.format("Invalid file in attachment: %s\n", args[start]);
				return;
			}
		}

		private void attachPipe(String s) {
			System.err.format("Pipe attachments not yet implemented: %s\n", s);
		}

		private void attachClass(Properties props, String s) {
			String[] args = s.split("\\s");
			for (int x = 1; x < args.length; ++x) {
				if (args[x].startsWith(">")) {
					excl = false;
					args[x] = args[x].substring(1);
					setupFile(args, x);
					// TODO: truncate args so Class doesn't see?
				}
			}
			Vector<String> argv = new Vector<String>(Arrays.asList(args));
			// try to construct from class...
			try {
				Class<?> clazz = Class.forName(args[0]);
				Constructor<?> ctor = clazz.getConstructor(
						Properties.class,
						argv.getClass(),
						VirtualUART.class);
				// funky "new" avoids "argument type mismatch"...
				Object obj = ctor.newInstance(
						props,
						argv,
						(VirtualUART)this);
				if (attach(obj)) {
					System.err.format("%s-%c attached to \"%s\"\n",
						name, index + 'A', s);
				} else {
					System.err.format("%s-%c failed to attached\n",
						name, index + 'A');
				}
			} catch (Exception ee) {
				System.err.format("Invalid class in attachment: %s\n", s);
				return;
			}
		}

		// Conditions affecting interrupts have changed, ensure proper signal.
		private void chkIntr() {
			int intr = 0;
			// TODO: determine interrupt status
			// TODO: "first char" handling needed?
			if ((wr[1] & 0x18) != 0 && (rr[0] & rr0_rxr_c) != 0) {
				intr |= 1;
			}
			if ((wr[1] & 0x02) != 0 && (rr[0] & rr0_txp_c) != 0) {
				intr |= 2;
			}
			// CTS/DCD detection done in setModem()
			if ((wr[1] & 0x01) != 0 && (rr[0] & 0xd0) != 0) {
				intr |= 4;
			}
			// TODO: special receive condition detection
			// TODO: exclude parity if bit set...
			if ((wr[1] & 0x18) != 0 && (rr[1] & 0xf0) != 0) {
				intr |= 8;
			}
			// TODO: check diffs?
			updateIntr(intr);
		}

		private void updateIntr(int intr) {
			intrs = intr;
			// TODO: set Ch A RR[0] bit D1...
			if (intrs != 0) {
				if (chA == null) rr[0] |= rr0_int_c;
				raiseINT(index);
			} else {
				if (chA == null) rr[0] &= ~rr0_int_c;
				lowerINT(index);
			}
		}

		public int in(int port) {
			int x = port & 1;
			int val = 0;
			if (x == 0) {	// data
				synchronized (this) {
					if (fifi.size() > 0) {
						try {
							val = fifi.take();
						} catch (Exception ee) {}
						if (fifi.size() == 0) {
							rr[0] &= ~rr0_rxr_c;
							chkIntr();
							wait.release();
						}
					}
				}
			} else {	// control
				int r = wr[0] & 0x07;
				if (r == 2) {
					setRR2();
				}
				val = rr[r] & 0xff;
				if (r != 0) {
					wr[0] &= ~0x07;
				}
				// never used? not working: need io.read() above...
				if (io_in && r == 0 && io.available() > 0) {
					val |= rr0_rxr_c;
				}
				// TODO: any required updates?
			}
			return val;
		}

		public void out(int port, int val) {
			int x = port & 1;
			val &= 0xff; // necessary?
			if (x == 0) {	// data
				if (attFile != null) {
					try {
						attFile.write(val);
					} catch (Exception ee) {}
				}
				if (io_out) {
					io.write(val);
				}
				if ((attFile == null && !io_out) || !excl) {
					synchronized (this) {
						fifo.add(val);
						if (attObj != null) {
							rr[0] &= ~rr0_txp_c;
						}
						// TODO: force Tx INT?
						chkIntr();
					}
				}
			} else {	// control
				int r = wr[0] & 0x07;
				wr[r] = (byte)val;
				if (r != 0) {
					wr[0] &= ~0x07;
				}
				// TODO: check for required updates...
				if (r == 0) {
					switch ((val >> 3) & 7) {
					case 0:
						break;
					case 1:
						break;
					case 2:	// reset ext/status
						updateIntr(intrs & ~4);
						rr[0] &= ~(rr0_cts_c | rr0_dcd_c);
						rr[0] |= (mdms & (rr0_cts_c | rr0_dcd_c));
						rr0Latch = false;
						break;
					case 3:
						reset(); // right?
						break;
					case 4:
						// TODO: INT on next Rx char
						break;
					case 5:
						updateIntr(intrs & ~2);
						break;
					case 6:
						// TODO: reset error bits
						break;
					case 7:
						retIntr(); // Ch A only...
						break;
					}
					switch ((val >> 6) & 3) {
					case 0:
						break;
					case 1:
						// TODO: reset Rx CRC
						break;
					case 2:
						// TODO: reset Tx CRC
						break;
					case 3:
						// TODO: reset Tx Underrun/EOM
						break;
					}
				} else if (r == 5) {
					updateModemOut();
				}
				// TODO: implement notification of modem lines...
				// attached object needs callback. Perhaps
				// combine with data stream via OOB values.
			}
		}

		public void reset() {
			fifo.clear();
			fifi.clear();
			Arrays.fill(wr, (byte)0);
			Arrays.fill(rr, (byte)0);
			rr[0] |= rr0_txp_c;
			rr[0] |= (mdms & (rr0_cts_c | rr0_dcd_c));
			rr0Latch = false;
			updateIntr(0);
			// TODO: chkIntr()? must exclude TxE
			// We essentially made space in Rx...
			// wake up sleeper...
			wait.release();
			updateModemForce();
		}

		private int getIntr() {
			int ret = -1;
			if ((intrs & 1) != 0) {	// Receive
				ret = 0b00000100;
			} else if ((intrs & 2) != 0) {	// Transmit
				ret = 0b00000000;
			} else if ((intrs & 4) != 0) { // Ext/Status
				ret = 0b00000010;
			} else if ((intrs & 8) != 0) { // Special Rcv
				ret = 0b00000110;
			}
			return ret;
		}

		private void setRR2() {
			if (chA == null) {
				return;
			}
			rr[2] = wr[2];
			if ((wr[1] & 0x04) == 0) { // not status-affects-vector
				return;
			}
			rr[2] &= 0b11110001;
			int mod = chA.getIntr();
			if (mod < 0) {
				mod = getIntr();
				if (mod < 0) {
					mod = 0b00000110;
				}
			} else {
				mod |= 0b00001000;
			}
			rr[2] |= (byte)mod;
		}

		// Only called on Ch B (!)
		public int readDataBus() {
			setRR2();
			return rr[2] & 0xff;
		}

		public void retIntr() {
			chkIntr();
		}

		////////////////////////////////////////////////////
		/// Interfaces for the virtual peripheral device ///
		public int available() {
			return fifo.size();
		}

		// Must sleep if nothing available...
		public int take() {
			try {
				int c = fifo.take(); // might sleep here...
				// TODO: how does this work with baud rate?
				if (fifo.size() == 0 || attObj == null) {
					synchronized (this) {
						rr[0] |= rr0_txp_c;
						chkIntr();
					}
				}
				return c;
			} catch(Exception ee) {
				// let caller do detach?
				return -1;
			}
		}

		public synchronized boolean ready() {
			// TODO: allow some buffering?
			// return (fifi.size() < N);
			// We are ready to PUT when RxR is 0...
			return (rr[0] & rr0_rxr_c) == 0;
		}

		public void put(int ch, boolean sleep) {
			// TODO: prevent infinite growth?
			// This must happen outside 'synchronized' block
			wait.drainPermits();
			while (sleep && attObj != null && !ready()) {
				try {
					wait.acquire();
				} catch (Exception ee) {}
			}
			if (attObj == null) {
				return;
			}
			synchronized (this) {
				fifi.add(ch & 0xff);
				lastRx = System.nanoTime();
				rr[0] |= rr0_rxr_c;
				chkIntr();
			}
		}
		public void setBaud(int baud) {
			// TODO: implement something.
			// Needed for SYNC modes.
		}

		public void setModem(int mdm) {
			int nuw = 0;
			if ((mdm & VirtualUART.SET_CTS) != 0) {
				nuw |= rr0_cts_c;
			}
			if ((mdm & VirtualUART.SET_DCD) != 0) {
				nuw |= rr0_dcd_c;
			}
			int diff = (mdms ^ nuw) & (rr0_cts_c | rr0_dcd_c);
			mdms &= ~(rr0_cts_c | rr0_dcd_c);
			mdms |= nuw;
			// TODO: these bits latch, must maintain separate static state...
			if (!rr0Latch && diff != 0) {
				rr[0] &= ~(rr0_cts_c | rr0_dcd_c);
				rr[0] |= nuw;
				rr0Latch = true;
			}
			if ((wr[1] & 0x01) != 0 && diff != 0) {
				// TODO: must make this thread-safe...
				updateIntr(intrs | 4);
			}
		}
		// For some reason, "synchronized" is required to ensure
		// we always return the latest values. Probably don't
		// need to mutex with data I/O, but this is easiest.
		public synchronized int getModem() {
			int mdm = 0;
			if ((wr[5] & wr5_dtr_c) != 0) {
				mdm |= VirtualUART.GET_DTR;
			}
			if ((wr[5] & wr5_rts_c) != 0) {
				mdm |= VirtualUART.GET_RTS;
			}
			if ((wr[5] & wr5_brk_c) != 0) {
				mdm |= VirtualUART.GET_BREAK;
			}
			if ((mdms & rr0_cts_c) != 0) {
				mdm |= VirtualUART.SET_CTS;
			}
			if ((mdms & rr0_dcd_c) != 0) {
				mdm |= VirtualUART.SET_DCD;
			}
			return mdm;
		}
		public boolean attach(Object periph) {
			if (attObj != null) {
				return false;
			}
			attObj = periph;
			return true;
		}
		public void detach() {
			System.err.format("%s-%c detaching peripheral\n",
						name, index + 'A');
			attObj = null;
			io = null;
			io_in = false;
			io_out = false;
			wait.release();
			try {
				fifo.addFirst(-1);
			} catch (Exception ee) {
				fifo.add(-1);
			}
		}

		public void attachDevice(SerialDevice io) {
			this.io = io;
			if (io == null) {
				io_in = false;
				io_out = false;
				return;
			}
			io_in = (io != null && (io.dir() & SerialDevice.DIR_IN) != 0);
			io_out = (io != null && (io.dir() & SerialDevice.DIR_OUT) != 0);
			updateModemForce();
		}
		public String getPortId() {
			return String.format("%s%c", name, index + 'A');
		}

		// Force a changeModem() event.
		private void updateModemForce() {
			modem = getModem() ^ VirtualUART.GET_ONLY; // force event?
			updateModemOut();
		}

		private void updateModemOut() {
			// only SerialDevice can handle these events...
			if (io == null) return;
			int mdm = modem & ~VirtualUART.GET_ONLY;
			if ((wr[5] & wr5_dtr_c) != 0) {
				mdm |= VirtualUART.GET_DTR;
			}
			if ((wr[5] & wr5_rts_c) != 0) {
				mdm |= VirtualUART.GET_RTS;
			}
			if ((wr[5] & wr5_brk_c) != 0) {
				mdm |= VirtualUART.GET_BREAK;
			}
			int diff = (mdm ^ modem) & VirtualUART.GET_ONLY;
			modem = mdm;
			if (diff != 0) {
				io.modemChange(this, modem);
			}
		}

		public String getDeviceName() { return name; }

		public String dumpDebug() {
			String ret = new String();
			ret += String.format("ch %c, #fifo = %d, #fifi = %d\n" +
				"WR0=%02x WR1=%02x WR2=%02x WR3=%02x\n" +
				"WR4=%02x WR5=%02x WR6=%02x WR7=%02x\n" +
				"RR0=%02x RR1=%02x RR2=%02x RR3=%02x\n",
				index + 'A', fifo.size(), fifi.size(),
				wr[0], wr[1], wr[2], wr[3],
				wr[4], wr[5], wr[6], wr[7],
				rr[0], rr[1], rr[2], rr[3]);
			ret += String.format("Modem=%04x latched=%s\n", mdms, rr0Latch);
			if (io != null) {
				ret += io.dumpDebug();
			}
			return ret;
		}
	}

	public String dumpDebug() {
		String ret = new String();
		ret += String.format("port %02x\n", basePort);
		ret += ports[0].dumpDebug();
		ret += '\n';
		ret += ports[1].dumpDebug();
		return ret;
	}
}
