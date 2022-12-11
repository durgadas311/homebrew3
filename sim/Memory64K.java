// Copyright (c) 2022 Douglas Miller <durgadas311@gmail.com>

import java.util.Arrays;
import java.util.Properties;

public class Memory64K extends Roms implements Memory, IODevice {
	private byte[] mem;

	public Memory64K(Properties props, String defrom) {
		super();
		mem = new byte[64*1024];
		Arrays.fill(mem, (byte)0);	// SRAM powers up 00?
		reset();
		initRom(props, defrom, mem, 0);
	}

	public int read(boolean rom, int bank, int address) {
		address &= 0xffff;
		return mem[address] & 0xff;
	}
	public int read(int address) {
		return read(true, 0, address);
	}
	public void write(int address, int value) {
		address &= 0xffff;
		if (address <= monMask) return;
		mem[address] = (byte)value;
	}

	public void reset() {
	}

	public int getBaseAddress() {
		return 0x38;
	}

	public int getNumPorts() {
		return 1;
	}

	public int in(int port) {
		return 0;
	}

	public void out(int port, int value) {
	}

	public String getDeviceName() {
		return "Memory64K";
	}

	public String dumpDebug() {
		return "";
	}
}
