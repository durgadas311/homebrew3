// Copyright 2018 Douglas Miller <durgadas311@gmail.com>

import java.util.Arrays;
import java.util.Vector;
import java.util.Map;
import java.util.HashMap;
import java.io.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Properties;

import z80core.*;

public class HomeBrew3 implements Computer, ComputerSystem,
			Commander, Interruptor, Runnable {
	private Z80 cpu;
	private Z80SIO sio;
	private long clock;
	private Map<Integer, IODevice> ios;
	private Vector<IODevice> devs;
	private Vector<DiskController> dsks;
	private Vector<InterruptController> intrs;
	private Memory mem = null;
	private boolean running;
	private boolean stopped;
	private Semaphore stopWait;
	private boolean tracing;
	private int traceCycles;
	private int traceLow;
	private int traceHigh;
	private int[] intRegistry;
	private int[] intLines;
	private int intState;
	private int intMask;
	private boolean nmiState;
	private boolean isHalted;
	private boolean sleeping;
	private Vector<ClockListener> clks;
	private Z80Disassembler disas;
	private StdioDebugger dbg;
	private ReentrantLock cpuLock;

	private int mdmA = 0;
	private int mdmB = 0;

	// Relationship between virtual CPU clock and real time
	// TODO: software select 4/8 MHz...
	private long intervalTicks = 4000;	// 1ms at 4MHz
	private long intervalNs = 1000000;	// 1ms
	private long backlogTime = 10000000;	// 10ms backlog limit
	private long backlogNs;

	public HomeBrew3(Properties props) {
		String s;
		intRegistry = new int[8];
		intLines = new int[8];
		Arrays.fill(intRegistry, 0);
		Arrays.fill(intLines, 0);
		intState = 0;
		intMask = 0;
		running = false;
		stopped = true;
		stopWait = new Semaphore(0);
		cpuLock = new ReentrantLock();
		sleeping = false;
		cpu = new Z80(this);
		ios = new HashMap<Integer, IODevice>();
		devs = new Vector<IODevice>();
		dsks = new Vector<DiskController>();
		clks = new Vector<ClockListener>();
		intrs = new Vector<InterruptController>();
		// Do this early, so we can log messages appropriately.
		s = props.getProperty("log");
		if (s != null) {
			String[] args = s.split("\\s");
			boolean append = false;
			for (int x = 1; x < args.length; ++x) {
				if (args[x].equals("a")) {
					append = true;
				}
			}
			try {
				FileOutputStream fo = new FileOutputStream(args[0], append);
				PrintStream ps = new PrintStream(fo, true);
				System.setErr(ps);
			} catch (Exception ee) {}
		}
		s = props.getProperty("configuration");
		if (s == null) {
			System.err.format("No config file found\n");
		} else {
			System.err.format("Using configuration from %s\n", s);
		}
		s = props.getProperty("con_att");
		if (s == null) {
			props.setProperty("con_att", "StdioSerial");
		}

		s = props.getProperty("trace");
		if (s != null) {
			Vector<String> ret = new Vector<String>();
			traceCommand(s.split("\\s"), ret, ret);
			// TODO: log error?
		}

		Memory64K mx = new Memory64K(props, "homebrew3.rom");
		//addDevice(mx);
		mem = mx;
		long clk = 400;	// system clock period, nS
		Z80CTC ctc = new Z80CTC(props, 0x00, clk, this);
		sio = new Z80SIO(props, "con", "sys", 0x04, this);
		Z80PIO pio1 = new Z80PIO(props, null, null, 0x08, this);
		Z80PIO pio2 = new Z80PIO(props, null, null, 0x0c, this);
		addDevice(pio2);
		addDevice(pio1);
		addDevice(sio);
		addDevice(ctc);

		s = props.getProperty("disas");
		if (s != null && s.equalsIgnoreCase("zilog")) {
			disas = new Z80DisassemblerZilog(mem);
		} else {
			disas = new Z80DisassemblerMAC80(mem);
		}

		s = props.getProperty("debugger");
		if (s != null) {
			dbg = new StdioDebugger(props, this);
		}
	}

	public void setSwitches(int sw, boolean on) {
		int _a = mdmA;
		int _b = mdmB;
		int chA = 0;
		int chB = 0;
		switch (sw) {
		case SwitchProvider.TOGGLE0:
			chA = VirtualUART.SET_CTS;
			break;
		case SwitchProvider.TOGGLE1:
			chA = VirtualUART.SET_DCD;
			break;
		case SwitchProvider.TOGGLE2:
			chB = VirtualUART.SET_CTS;
			break;
		case SwitchProvider.TOGGLE3:
			chB = VirtualUART.SET_DCD;
			break;
		}
		if (chA != 0) {
			if (on) {
				mdmA |= chA;
			} else {
				mdmA &= ~chA;
			}
			if (_a != mdmA) {
				sio.portA().setModem(mdmA);
			}
		}
		if (chB != 0) {
			if (on) {
				mdmB |= chB;
			} else {
				mdmB &= ~chB;
			}
			if (_b != mdmB) {
				sio.portB().setModem(mdmB);
			}
		}
	}

	public void changeSpeed(int x,int y) {
	}

	// ComputerSystem interfaces:
	public void reset() {
		boolean wasRunning = running;
		tracing = false;
		traceCycles = 0;
		traceLow = 0;
		traceHigh = 0;
		// TODO: reset other interrupt state? devices should do that...
		intMask = 0;
		clock = 0;
		stop();
		if (false && wasRunning) {
			System.err.format("backlogNs=%d\n", backlogNs);
		}
		backlogNs = 0;
		cpu.reset();
		mem.reset();
		for (int x = 0; x < devs.size(); ++x) {
			devs.get(x).reset();
		}
		if (wasRunning) {
			start();
		}
	}
	public void dmaRead(byte[] buf, int adr, int len) {
		try {
			if (!sleeping) {
				cpuLock.lock(); // might wait for CPU to finish 1mS
			}
			int x = 0;
			while (len > 0 && x < buf.length) {
				buf[x++] = (byte)peek8(adr++);
				--len;
			}
		} finally {
			if (!sleeping) {
				cpuLock.unlock();
			}
		}
	}
	public void dmaWrite(byte[] buf, int adr, int len) {
		try {
			if (!sleeping) {
				cpuLock.lock(); // might wait for CPU to finish 1mS
			}
			int x = 0;
			while (len > 0 && x < buf.length) {
				poke8(adr++, buf[x++] & 0xff);
				--len;
			}
		} finally {
			if (!sleeping) {
				cpuLock.unlock();
			}
		}
	}
	////////////////////////////////////////////////////

	public boolean addDevice(IODevice dev) {
		if (dev == null) {
			System.err.format("NULL I/O device\n");
			return false;
		}
		int base = dev.getBaseAddress();
		int num = dev.getNumPorts();
		if (num <= 0) {
			System.err.format("No ports\n");
			return false;
		}
		for (int x = 0; x < num; ++x) {
			if (ios.get(base + x) != null) {
				System.err.format("Conflicting I/O %02x (%02x)\n", base, num);
				return false;
			}
		}
		devs.add(dev);
		for (int x = 0; x < num; ++x) {
			ios.put(base + x, dev);
		}
		return true;
	}

	public IODevice getDevice(int basePort) {
		IODevice dev = ios.get(basePort);
		return dev;
	}

	public Vector<DiskController> getDiskDevices() {
		return dsks;
	}

	public boolean addDiskDevice(DiskController dev) {
		if (!addDevice(dev)) {
			return false;
		}
		dsks.add(dev);
		return true;
	}

	// These must NOT be called from the thread...
	public void start() {
		stopped = false;
		if (running) {
			return;
		}
		running = true;
		Thread t = new Thread(this);
		t.setPriority(Thread.MAX_PRIORITY);
		t.start();
	}
	public void stop() {
		stopWait.drainPermits();
		if (!running) {
			return;
		}
		running = false;
		// This is safer than spinning, but still stalls the thread...
		try {
			stopWait.acquire();
		} catch (Exception ee) {}
	}
	private void addTicks(int ticks) {
		clock += ticks;
		for (ClockListener lstn : clks) {
			lstn.addTicks(ticks, clock);
		}
	}

	// I.e. admin commands to virtual system...
	public Commander getCommander() {
		return (Commander)this;
	}

	// TODO: these may be separate classes...

	/////////////////////////////////////////////
	/// Interruptor interface implementation ///
	public int registerINT(int irq) {
		int val = intRegistry[irq & 7]++;
		// TODO: check for overflow (32 bits max?)
		return val;
	}
	public void raiseINT(int irq, int src) {
		irq &= 7;
		intLines[irq] |= (1 << src);
		intState |= (1 << irq);
		if ((intState & ~intMask) != 0) {
			cpu.setINTLine(true);
		}
	}
	public void lowerINT(int irq, int src) {
		irq &= 7;
		intLines[irq] &= ~(1 << src);
		if (intLines[irq] == 0) {
			intState &= ~(1 << irq);
			if ((intState & ~intMask) == 0) {
				cpu.setINTLine(false);
			}
		}
	}
	public void blockInts(int msk) {
		intMask |= msk;
		if ((intState & ~intMask) == 0) {
			cpu.setINTLine(false);
		}
	}
	public void unblockInts(int msk) {
		intMask &= ~msk;
		if ((intState & ~intMask) != 0) {
			cpu.setINTLine(true);
		}
	}
	public synchronized void setNMI(boolean state) {
		if (isHalted && state) {
			cpu.triggerNMI();
		}
		nmiState = state;
	}
	// Not part of Interruptor interface.
	private synchronized void setHalted(boolean halted) {
		if (!isHalted && halted && nmiState) {
			cpu.triggerNMI();
		}
		isHalted = halted;
	}
	// does not allow direct access to NMI pin.
	public void triggerNMI() {
		cpu.triggerNMI();
	}
	public void addClockListener(ClockListener lstn) {
		clks.add(lstn);
	}
	public void addIntrController(InterruptController ctrl) {
		// Each Z80-compatible IM2 chip may register here.
		// Order is vital, establishes interrupt daisy-chain.
		intrs.add(ctrl);
	}
	public void waitCPU() {
		// Keep issuing clock cycles while stalling execution.
		addTicks(1);
	}
	public boolean isTracing() {
		return tracing;
	}
	public void startTracing() {
		tracing = true;
	}
	public void stopTracing() {
		tracing = false;
	}

	/////////////////////////////////////////////
	/// Commander interface implementation ///
	public Vector<String> sendCommand(String cmd) {
		// TODO: stop Z80 during command? Or only pause it?
		String[] args = cmd.split("\\s");
		Vector<String> ret = new Vector<String>();
		ret.add("ok");
		Vector<String> err = new Vector<String>();
		err.add("error");
		if (args.length < 1) {
			return ret;
		}
		if (args[0].equalsIgnoreCase("quit")) {
			// Release Z80, if held...
			stop();
			System.exit(0);
		}
		if (args[0].equalsIgnoreCase("trace") && args.length > 1) {
			if (!traceCommand(args, err, ret)) {
				return err;
			}
			return ret;
		}
		try {
			if (!sleeping) {
				cpuLock.lock(); // might wait for CPU to finish 1mS
			}
			if (args[0].equalsIgnoreCase("reset")) {
				reset();
				return ret;
			}
			if (args[0].equalsIgnoreCase("nmi")) {
				triggerNMI();
				return ret;
			}
			if (args[0].equalsIgnoreCase("getdevs")) {
				for (IODevice dev : devs) {
					String nm = dev.getDeviceName();
					if (nm != null) {
						ret.add(nm);
					}
				}
				return ret;
			}
			if (args[0].equalsIgnoreCase("dump") && args.length > 1) {
				if (args[1].equalsIgnoreCase("cpu")) {
					ret.add(cpu.dumpDebug());
					ret.add(disas.disas(cpu.getRegPC()) + "\n");
				}
				if (args[1].equalsIgnoreCase("page") && args.length > 2) {
					String s = dumpPage(args);
					if (s == null) {
						err.add("syntax");
						err.addAll(Arrays.asList(args));
						return err;
					}
					ret.add(s);
				}
				if (args[1].equalsIgnoreCase("mach")) {
					ret.add(dumpDebug());
				}
				if (args[1].equalsIgnoreCase("disk") && args.length > 2) {
					IODevice dev = findDevice(args[2]);
					if (dev == null) {
						err.add("nodevice");
						err.add(args[2]);
						return err;
					}
					ret.add(dev.dumpDebug());
				}
				return ret;
			}
			err.add("badcmd");
			err.add(cmd);
			return err;
		} finally {
			if (!sleeping) {
				cpuLock.unlock();
			}
		}
	}

	private boolean traceCommand(String[] args, Vector<String> err,
			Vector<String> ret) {
		// TODO: do some level of mutexing?
		if (args[1].equalsIgnoreCase("on")) {
			startTracing();
		} else if (args[1].equalsIgnoreCase("off")) {
			traceLow = traceHigh = 0;
			traceCycles = 0;
			stopTracing();
		} else if (args[1].equalsIgnoreCase("cycles") && args.length > 2) {
			try {
				traceCycles = Integer.valueOf(args[2]);
			} catch (Exception ee) {}
		} else if (args[1].equalsIgnoreCase("pc") && args.length > 2) {
			// TODO: this could be a nasty race condition...
			try {
				traceLow = Integer.valueOf(args[2], 16);
			} catch (Exception ee) {}
			if (args.length > 3) {
				try {
					traceHigh = Integer.valueOf(args[3], 16);
				} catch (Exception ee) {}
			} else {
				traceHigh = 0x10000;
			}
			if (traceLow >= traceHigh) {
				traceLow = traceHigh = 0;
			}
		} else {
			err.add("unsupported:");
			err.add(args[1]);
			return false;
		}
		return true;
	}

	private GenericDiskDrive findDrive(String name) {
		for (DiskController dev : dsks) {
			GenericDiskDrive drv = dev.findDrive(name);
			if (drv != null) {
				return drv;
			}
		}
		return null;
	}

	private IODevice findDevice(String name) {
		for (IODevice dev : devs) {
			if (name.equals(dev.getDeviceName())) {
				return dev;
			}
		}
		return null;
	}

	/////////////////////////////////////////
	/// Computer interface implementation ///

	public int peek8(int address) {
		int val = mem.read(address);
		return val;
	}
	public void poke8(int address, int value) {
//if ((address & 0xfffe) == 0xac00) {
//System.err.format("%04x: (%04x)=%02x\n", cpu.getRegPC(), address, value);
//}
		mem.write(address, value);
	}

	// fetch Interrupt Response byte, IM0 (instruction bytes) or IM2 (vector).
	// Implementation must keep track of multi-byte instruction sequence,
	// and other possible state. For IM0, Z80 will call as long as 'intrFetch' is true.
	public int intrResp(Z80State.IntMode mode) {
		if (mode != Z80State.IntMode.IM2) {
			// can only operate in IM2.(?)  IM1 should never call this.
			return 0; // TODO: What to return in this case?
		}
		int vector = -1;
		for (InterruptController ctrl : intrs) {
			vector = ctrl.readDataBus();
			if (vector >= 0) {
				return vector;
			}
		}
		// TODO: what if no device claimed interrupt?
		return vector;
	}

	public void retIntr(int opCode) {
		if (opCode != 0x4d) {
			// TODO: RETN doesn't matter?
			return;
		}
		for (InterruptController ctrl : intrs) {
			if (ctrl.retIntr()) {
				return;
			}
		}
	}

	public int inPort(int port) {
		int val = 0;
		port &= 0xff;
		IODevice dev = ios.get(port);
		if (dev == null) {
			// This helps U-ROM quickly decide WIN is missing
			val = 0xff;
			//System.err.format("Undefined Input on port %02x\n", port);
		} else {
			val = dev.in(port);
		}
		return val;
	}
	public void outPort(int port, int value) {
		port &= 0xff;
		IODevice dev = ios.get(port);
		if (dev == null) {
			//System.err.format("Undefined Output on port %02x value %02x\n", port, value);
		} else {
			dev.out(port, value);
		}
	}

	// No longer used...
	public void contendedStates(int address, int tstates) {
		addTicks(tstates);
	}
	// not used?
	public long getTStates() {
		return clock;
	}

	public void breakpoint() {
	}
	public void execDone() {
	}

	//////// Runnable /////////
	public void run() {
		String traceStr = "";
		int clk = 0;
		int limit = 0;
		while (running) {
			cpuLock.lock(); // This might sleep waiting for GUI command...
			limit += intervalTicks;
			long t0 = System.nanoTime();
			int traced = 0; // assuming any tracing cancels 2mS accounting
			while (running && limit > 0) {
				int PC = cpu.getRegPC();
				boolean trace = tracing;
				if (!trace && (traceCycles > 0 ||
						(PC >= traceLow && PC < traceHigh))) {
					trace = true;
					//trace = ((gpp.get() & 0x80) == 0);
				}
				if (trace) {
					++traced;
					traceStr = String.format("{%05d} %04x: %02x %02x %02x %02x " +
						": %02x %04x %04x %04x [%04x] <%02x/%02x>%s",
						clock & 0xffff,
						PC, mem.read(PC), mem.read(PC + 1),
						mem.read(PC + 2), mem.read(PC + 3),
						cpu.getRegA(),
						cpu.getRegBC(),
						cpu.getRegDE(),
						cpu.getRegHL(),
						cpu.getRegSP(),
						intState, intMask,
						cpu.isINTLine() ? " INT" : "");
				}
				clk = cpu.execute();
				if (clk < 0) {
					clk = -clk;
					if (trace) {
						System.err.format("%s {%d} *INTA*\n",
							traceStr, clk);
					}
				} else if (trace) {
					// TODO: collect data after instruction?
					System.err.format("%s {%d} %s\n", traceStr, clk,
						disas.disas(PC));
				}

				setHalted(cpu.isHalted());
				limit -= clk;
				if (traceCycles > 0) {
					traceCycles -= clk;
				}
				addTicks(clk);
			}
			cpuLock.unlock();
			if (!running) {
				break;
			}
			long t1 = System.nanoTime();
			if (traced == 0) {
				backlogNs += (intervalNs - (t1 - t0));
				t0 = t1;
				if (backlogNs > backlogTime) { 
					try {
						Thread.sleep(backlogTime / 1000000);
					} catch (Exception ee) {}
					t1 = System.nanoTime();
					backlogNs -= (t1 - t0);
				}
			}
			t0 = t1;
		}
		stopped = true;
		stopWait.release();
	}

	public String dumpPage(String[] args) {
		String str = "";
		int pg = 0;
		int bnk = 0;
		int i = 2;
		boolean rom = false;
		if (args[i].equalsIgnoreCase("rom")) {
			rom = true;
			++i;
		}
		if (args.length - i > 1) {
			try {
				bnk = Integer.valueOf(args[i++]);
			} catch (Exception ee) {
				return ee.getMessage();
			}
		}
		if (args.length - i < 1) {
			return null;
		}
		try {
			pg = Integer.valueOf(args[i++], 16);
		} catch (Exception ee) {
			return ee.getMessage();
		}
		int adr = pg << 8;
		int end = adr + 0x0100;
		while (adr < end) {
			str += String.format("%04x:", adr);
			for (int x = 0; x < 16; ++x) {
				str += String.format(" %02x", mem.read(rom, bnk, adr + x));
			}
			str += "  ";
			for (int x = 0; x < 16; ++x) {
				int c = mem.read(rom, bnk, adr + x);
				if (c < ' ' || c > '~') {
					c = '.';
				}
				str += String.format("%c", (char)c);
			}
			str += '\n';
			adr += 16;
		}
		return str;
	}

	public String dumpDebug() {
		String ret = ""; // gpp.dumpDebug();
		ret += String.format("CLK %d", getTStates());
		if (running) {
			ret += " RUN";
		}
		if (stopped) {
			ret += " STOP";
		}
		if (!running && !stopped) {
			ret += " limbo";
		}
		ret += "\n";
		ret += String.format("CPU Backlog = %d nS\n", backlogNs);
		ret += "INT = {";
		for (int x = 0; x < 8; ++x) {
			ret += String.format(" %x", intLines[x]);
		}
		ret += String.format(" } %02x %02x\n", intState, intMask);
		return ret;
	}
}
