// Copyright (c) 2017 Douglas Miller <durgadas311@gmail.com>

import java.util.Arrays;
import java.util.Vector;
import java.util.Properties;
import java.awt.event.*;
import java.io.*;
import java.util.concurrent.Semaphore;
import java.lang.reflect.Constructor;

public class Z80CTC implements IODevice, InterruptController {
	static final int fifoLimit = 10; // should never even exceed 2
	private Interruptor intr;
	private int src;
	private int basePort;
	private String name = null;

	private Z80CTCchan[] chans = new Z80CTCchan[4];
	private int intrs = 0;
	private long phi;

	public Z80CTC(Properties props, int base, long clk, Interruptor intr) {
		name = "Z80CTC";
		this.intr = intr;
		src = intr.registerINT(0);
		intr.addIntrController(this);
		basePort = base;
		phi = clk;
		chans[0] = new Z80CTCchan(props, 0, null);
		chans[1] = new Z80CTCchan(props, 1, chans[0]);
		chans[2] = new Z80CTCchan(props, 2, chans[0]);
		chans[3] = new Z80CTCchan(props, 3, chans[0]);
		reset();
	}

	// System clock period, nS.
	public void setClock(long ns) {
		phi = ns;
		// TODO: trigger re-compute on each channel...
		// may not be necessary since this can only happen
		// at system RESET(?) Or else if CTC is always
		// re-programmed.
	}

	///////////////////////////////
	/// Interfaces for IODevice ///
	public int in(int port) {
		int x = port & 3;
		return chans[x].in(port);
	}

	public void out(int port, int val) {
		int x = port & 3;
		chans[x].out(port, val);
	}

	public void reset() {
		intrs = 0;
		intr.lowerINT(0, src);
		chans[0].reset();
		chans[1].reset();
		chans[2].reset();
		chans[3].reset();
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

	public TimerCounterChannel chan0() { return chans[0]; }
	public TimerCounterChannel chan1() { return chans[1]; }
	public TimerCounterChannel chan2() { return chans[2]; }
	public TimerCounterChannel chan3() { return chans[3]; }

	public int readDataBus() {
		if (intrs == 0) {
			return -1;
		}
		int vec = -1;
		int x = 0;	// ch 0 is highest priority...
		while (vec < 0 && x < 4) {
			vec = chans[x++].readDataBus();
		}
		return vec;
	}

	public boolean retIntr() {
		if (intrs == 0) {
			return false;
		}
		if ((intrs & 1) != 0) {
			chans[0].retIntr();
		} else if ((intrs & 2) != 0) {
			chans[1].retIntr();
		} else if ((intrs & 4) != 0) {
			chans[2].retIntr();
		} else if ((intrs & 8) != 0) {
			chans[3].retIntr();
		}
		if (intrs == 0) {
			intr.lowerINT(0, src);
		}
		return true;
	}

	class Z80CTCchan implements TimerCounterChannel, ActionListener {
		private byte cfg;
		private byte vec;
		private byte tcn;
		private byte cnt;

		static final int cfg_ctl_c = 0x01;
		static final int cfg_rst_c = 0x02;
		static final int cfg_tcn_c = 0x04;
		static final int cfg_trg_c = 0x08;
		static final int cfg_edg_c = 0x10;
		static final int cfg_pre_c = 0x20;
		static final int cfg_mod_c = 0x40;
		static final int cfg_int_c = 0x80;

		private TimerCounter outA;
		private BaudListener outB;
		private long nanoBaud = 0; // length of char in nanoseconds
		private long period = 0; // length of clock period
		private Z80CTCchan ch0; // null on Ch 0
		private int index;
		private int intrs;
		private boolean tcn_follows;
		private javax.swing.Timer timer;

		public Z80CTCchan(Properties props, int idx, Z80CTCchan alt) {
			ch0 = alt;
			index = idx;
			tcn_follows = false;
			timer = null;
		}

		public void attach(TimerCounter tcr) {
			outA = tcr;
		}

		public void attach(BaudListener bdl) {
			outB = bdl;
		}

		private void updateIntr(int intr) {
			intrs = intr;
			// TODO: anything else to set?
			if (intrs != 0) {
				raiseINT(index);
			} else {
				lowerINT(index);
			}
		}

		public int in(int port) {
			int val = 0;
			// TODO: compute current value of counter...
			val = cnt & 0xff;
			return val;
		}

		private void setupTimer(long clk) {
			long tc = clk;
			if ((cfg & cfg_mod_c) == 0) {	// timer...
				tc *= ((cfg & cfg_pre_c) == 0 ? 16 : 256);
			}
			tc *= (tcn & 0xff);
			if (outA != null) {
				outA.setPeriod(tc);
				// TODO: skip local timer then?
				if ((cfg & cfg_int_c) == 0) {
					return;
				}
			}
			if (outB != null) {
				// TODO: compute baud...
				int baud = 0;
				outB.setBaud(baud);
			}
			// we hope this is an integral mS value...
			tc /= 1000000;	// to mS...
			if (timer != null) {
				// is this enough?
				timer.removeActionListener(this);
				timer = null;
			}
			if ((cfg & cfg_int_c) != 0 && tc > 0) {
				timer = new javax.swing.Timer((int)tc, this);
				timer.start(); // or later???
			}
		}

		public void out(int port, int val) {
			if (tcn_follows) {
				tcn = (byte)val;
				cnt = tcn;
				tcn_follows = false;
				if ((cfg & cfg_mod_c) == 0) {	// timer...
					setupTimer(phi);
				} else if (period > 0) {
					setupTimer(period);
				}
				// TODO: recompute timers, etc.
			} else if ((val & cfg_ctl_c) == 0) {
				vec = (byte)val;
			} else { // channel config...
				cfg = (byte)val;
				tcn_follows = ((cfg & cfg_tcn_c) != 0);
				if ((cfg & cfg_rst_c) != 0) {
					// TODO: reset timer/counter
				}
				if ((cfg & cfg_tcn_c) == 0) {
					if ((cfg & cfg_mod_c) == 0) {	// timer...
						setupTimer(phi);
					} else if (period > 0) {
						setupTimer(period);
					}
				}
				// TODO: perform any setup required now.
			}
		}

		public void reset() {
			cfg &= ~cfg_int_c;
			if (timer != null) {
				// is this enough?
				timer.removeActionListener(this);
				timer = null;
			}
			updateIntr(0);
			// TODO: more?
		}

		public int readDataBus() {
			if (intrs == 0) {
				return -1;
			}
			if (ch0 != null) {
				vec = ch0.vec;
			}
			vec &= 0b11111001;
			vec |= (index << 1);
			return vec & 0xff;
		}

		public void retIntr() {
			// TODO: handle restart of continuous, etc.
			updateIntr(0);
		}

		public void setPeriod(long ns) {	// clock period, nS
			period = ns;
			setupTimer(period); // might need to be redone later...
		}

		public void setLine(boolean ln) {	// trigger on/off (hi/lo, 1/0, ...)
			if (timer != null) {
				return;
			}
			// TODO: select edge to detect
			if (!ln) {
				return;
			}
			--cnt;
			if (cnt == 0) {
				if ((cfg & cfg_int_c) != 0) {
					updateIntr(1);
				}
				cnt = tcn;
			}
			// TODO: more?
		}

		public void actionPerformed(ActionEvent e) {
			// if continuous, reset timer. else cancel.
			if ((cfg & cfg_int_c) != 0) {
				updateIntr(1);
			}
			if (outA != null) {
				outA.setLine(true); // needs toggle...
				outA.setLine(false);
			}
		}

		public String getDeviceName() { return name; }

		public String dumpDebug() {
			String ret = new String();
			ret += String.format("ch %d: intr=%d " +
				"CTL=%02x VEC=%02x TCN=%02x count=%02x\n",
				index, intrs,
				cfg, vec, tcn, 0);
			return ret;
		}
	}

	public String dumpDebug() {
		String ret = new String();
		ret += String.format("port %02x intrs=%d\n", basePort, intrs);
		ret += chans[0].dumpDebug();
		ret += chans[1].dumpDebug();
		ret += chans[2].dumpDebug();
		ret += chans[3].dumpDebug();
		return ret;
	}
}
