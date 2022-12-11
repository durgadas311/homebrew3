// Copyright (c) 2018 Douglas Miller <durgadas311@gmail.com>

import java.util.Arrays;
import java.util.Properties;

public class Memory256K extends Roms implements Memory, IODevice {
	private byte[] mem;
	boolean enable;
	protected int[] rmu;	// maps for 16K regions
	protected int[] wmu;	// maps for 16K regions

	public Memory256K(Properties props, String defrom) {
		super();
		mem = new byte[256*1024];
		Arrays.fill(mem, (byte)0);	// SRAM powers up 00?
		rmu = new int[4];
		wmu = new int[4];
		reset();
		initRom(props, defrom, mem, rmu[0] << 14);
	}

	public int read(boolean rom, int bank, int address) {
		address &= 0x3fff;
		address += (bank << 14);
		return mem[address & 0x3ffff] & 0xff;
	}
	public int read(int address) {
		address &= 0xffff;
		return read(enable, rmu[address >> 14], address);
	}
	public void write(int address, int value) {
		address &= 0xffff;
		int bank = wmu[address >> 14];
		address &= 0x3fff;
		address += bank << 14;
		// High 128K is ROM...
		if (address < 0x20000) {
			mem[address & 0x3ffff] = (byte)value;
		}
	}

	public void reset() {
		enable = false;
		rmu[0] = 15;
		wmu[0] = 0;
		wmu[1] = rmu[1] = 1;
		wmu[2] = rmu[2] = 2;
		wmu[3] = rmu[3] = 3;
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
		enable = (value & 0x80) == 0;
		int cnk = (value >> 4) & 3;
		wmu[cnk] = rmu[cnk] = value & 0x0f;
	}

	public String getDeviceName() {
		return "Memory256K";
	}

	public String dumpDebug() {
		String str = String.format("en=%s\n" +
					"mmu=%2d %2d %2d %2d RD\n" +
					"    %2d %2d %2d %2d WR\n",
			enable, rmu[0], rmu[1], rmu[2], rmu[3],
			wmu[0], wmu[1], wmu[2], wmu[3]);
		return str;
	}
}
