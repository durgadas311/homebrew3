// Copyright (c) 2017 Douglas Miller <durgadas311@gmail.com>

import java.io.*;
import java.util.Properties;

public class Roms {
	protected int monMask;

	public Roms() {
	}

	protected void initRom(Properties props, String defRom, byte[] mem, int rombase) {
		InputStream fi = null;
		String s = props.getProperty("monitor_rom");
		if (s == null) {
			System.err.format("No Monitor ROM specified, using %s\n", defRom);
			s = defRom;
		}
		try {
			fi = new FileInputStream(s);
		} catch (FileNotFoundException nf) {
			try {
				fi = this.getClass().getResourceAsStream(s);
			} catch (Exception ee) {
				ee.printStackTrace();
				System.exit(1);
			}
		} catch (Exception ee) {
			ee.printStackTrace();
			System.exit(1);
		}
		try {
			// TODO: check for power-of-two and viable ROM sizes
			monMask = fi.available() - 1;
			fi.read(mem, rombase, fi.available());
			fi.close();
			System.err.format("Monitor ROM %s %04x\n", s, monMask);
		} catch (Exception ee) {
			ee.printStackTrace();
			System.exit(1);
		}
	}
}
