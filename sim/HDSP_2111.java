// Copyright 2022 Douglas Miller <durgadas311@gmail.com>

import java.util.Arrays;
import java.util.Properties;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

class HDSP_2111 extends JTextArea
		implements IODevice {
	private byte[] cg;	// fixed character-generator data
	private byte[] dsp;
	private byte[] fls;
	private byte[] udc;	// user-defined char-gen data
	private byte[][] frame; // buffer of data to paint()
	private byte ctlWord;
	private int udcAddr;
	private Font dspFont;
	private Color bzl = new Color(50, 0, 0);

	public HDSP_2111(Properties props, int base) {
		// super(); not needed?
		setForeground(Color.red);
		setBackground(bzl);
		setPreferredSize(new Dimension(300, 50));
		setOpaque(true);
		setEditable(false);
		setMargin(new Insets(6, 8, 6, 8));
		cg = new byte[1024];
		try {
			InputStream fi = this.getClass().getResourceAsStream("fonts/hdsp-2111.bin");
			fi.read(cg);
			fi.close();
			fi = this.getClass().getResourceAsStream("fonts/GEN5x7.ttf");
			dspFont = Font.createFont(Font.TRUETYPE_FONT, fi);
			dspFont = dspFont.deriveFont(4f);
			setFont(dspFont);
		} catch (Exception ee) {
			ee.printStackTrace();
			System.exit(1);
		}
		dsp = new byte[8];
		fls = new byte[8];
		udc = new byte[16*8];
		reset();
		String s = props.getProperty("display_test");
		if (s != null) {
			int x = 0;
			for (byte b : s.getBytes()) {
				dsp[x++] = b;
				if (x >= 8) break;
			}
			update(-1);
		}
	}

	public void reset() {
		ctlWord = 0;
		clear();
	}

	private void clear() {
		Arrays.fill(dsp, (byte)' ');
		Arrays.fill(fls, (byte)0);
		update(-1);
	}

	private void update(int scope) {
		int x, y, r;
		String d = "";
		for (x = 0; x < 7; ++x) { // rows
			for (y = 0; y < 8; ++y) { // chars/cols
				int c = dsp[y];
				boolean u = ((c & 0x80) != 0);
				if (u) { // UDC
					r = ((c & 0x0f) << 3) + x;
					d += (char)(' ' | (udc[r] & 0x1f));
				} else {
					r = (c << 3) + 1 + x;
					d += (char)(' ' | (cg[r] & 0x1f));
				}
			}
			d += '\n'; // TODO: not last row?
		}
		setText(d);
	}

	public int getBaseAddress() { return 0x80; }
	public int getNumPorts() { return 64; }

	public int in(int port) {
		if ((port & 040) == 0) { // flash (blink) RAM
			return fls[port & 007];
		}
		switch (port & 070) {
		case 070:	// display RAM
			return dsp[port & 007] & 0xff;
		case 060:	// control word
			return ctlWord & 0xff;
		case 050:	// UDC RAM
			return udc[udcAddr + (port & 7)] & 0xff;
		case 040:	// UDC addr
			return (udcAddr >> 3) & 0x0f;
		}
		return 0;
	}

	public void out(int port, int val) {
		if ((port & 040) == 0) { // flash (blink) RAM
			fls[port & 007] = (byte)(val & 1);
			return;
		}
		switch (port & 070) {
		case 070:	// display RAM
			dsp[port & 007] = (byte)val;
			update(port & 007);
			break;
		case 060:	// control word
			ctlWord = (byte)val;
			if ((ctlWord & 0x80) != 0) {
				clear();
				ctlWord &= 0x7f;
			}
			update(-1);
			break;
		case 050:	// UDC RAM
			udc[udcAddr + (port & 7)] = (byte)(val & 0x1f);
			// TODO: updates?
			break;
		case 040:	// UDC addr
			udcAddr = (val & 0x0f) << 3;
			break;
		}
	}

	public String getDeviceName() { return "HDSP-2111"; }

	public String dumpDebug() {
		return "";
	}
}
