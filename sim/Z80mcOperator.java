// Copyright (c) 2016 Douglas Miller
import java.awt.event.*;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.Map;
import java.util.HashMap;
import java.util.Vector;
import java.util.Properties;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

public class Z80mcOperator
	implements ActionListener, Runnable
{
	JFrame _main;
	Commander _cmdr = null;
	LEDHandler _ledhandler = null;
	JMenuBar _mb;
	JMenu _sys_mu;
	JMenu _dbg_mu;
	int _reset_key;
	int _tracecust_key;
	int _traceon_key;
	int _traceoff_key;
	int _dump_key;
	JMenuItem _dump_mi;
	int _cpu_key;
	int _mach_key;
	int _page_key;
	int _quit_key;
	int _key;

	Object[] trace_btns;
	JTextField trace_cyc;
	JTextField trace_lo;
	JTextField trace_hi;
	JTextField trace_sec;
	JPanel trace_pn;
	JPanel trace_cyc_pn;
	JPanel trace_lo_pn;
	JPanel trace_hi_pn;
	JPanel trace_sec_pn;
	JPanel dmppg_pn;
	JPanel dump_pg_pn;
	JPanel dump_bnk_pn;
	JTextField dump_pg;
	JTextField dump_bnk;
	JCheckBox dump_rom;
	static final int OPTION_CANCEL = 0;
	static final int OPTION_YES = 1;

	Map<Integer, String> _devs;
	Map<Integer, JMenuItem> _mnus;
	private java.util.concurrent.LinkedBlockingDeque<Integer> _cmds;
	static final String rule = "--------------------------------------------------------------------------------";
	private boolean dumpToLog = false;

	GenericHelp _help;

	Z80mcOperator(JFrame main, Properties props, LEDHandler lh) {
		_main = main;
		_ledhandler = lh;
		_key = 1;
		_devs = new HashMap<Integer, String>();
		_mnus = new HashMap<Integer, JMenuItem>();
		_cmds = new java.util.concurrent.LinkedBlockingDeque<Integer>();

		if (main == null) {
			return;
		}

		JMenuBar _mb = new JMenuBar();

		_sys_mu = new JMenu("System");
		JMenuItem mi;
		_reset_key = _key++;
		mi = new JMenuItem("Reset", _reset_key);
		mi.addActionListener(this);
		_sys_mu.add(mi);
		_quit_key = _key++;
		mi = new JMenuItem("Quit", _quit_key);
		mi.addActionListener(this);
		_sys_mu.add(mi);
		_mb.add(_sys_mu);

		_dbg_mu = new JMenu("Debug");
		_tracecust_key = _key++;
		mi = new JMenuItem("Trace (custom)", _tracecust_key);
		mi.addActionListener(this);
		_dbg_mu.add(mi);
		_traceon_key = _key++;
		mi = new JMenuItem("Trace ON", _traceon_key);
		mi.addActionListener(this);
		_dbg_mu.add(mi);
		_traceoff_key = _key++;
		mi = new JMenuItem("Trace OFF", _traceoff_key);
		mi.addActionListener(this);
		_dbg_mu.add(mi);
		dumpToLog = false;
		_dump_key = _key++;
		_dump_mi = new JMenuItem("Dump To Log", _dump_key);
		_dump_mi.addActionListener(this);
		_dbg_mu.add(_dump_mi);
		_cpu_key = _key++;
		mi = new JMenuItem("Dump CPU", _cpu_key);
		mi.addActionListener(this);
		_dbg_mu.add(mi);
		_mach_key = _key++;
		mi = new JMenuItem("Dump Machine", _mach_key);
		mi.addActionListener(this);
		_dbg_mu.add(mi);
		_page_key = _key++;
		mi = new JMenuItem("Dump Page", _page_key);
		mi.addActionListener(this);
		_dbg_mu.add(mi);
		// More added when computer connected
		_mb.add(_dbg_mu);

		JMenu mu = new JMenu("Help");
		mi = new JMenuItem("About", KeyEvent.VK_A);
		mi.addActionListener(this);
		mu.add(mi);
		mi = new JMenuItem("Help", KeyEvent.VK_H);
		mi.addActionListener(this);
		mu.add(mi);
		_mb.add(mu);

		main.setJMenuBar(_mb);

		java.net.URL url = this.getClass().getResource("docs/Z80mc.html");
		_help = new GenericHelp(main.getTitle() + " Help", url);

		// Dialog for trace (custom)...
		trace_pn = new JPanel();
		trace_pn.setLayout(new BoxLayout(trace_pn, BoxLayout.Y_AXIS));
		trace_btns = new Object[2];
		trace_btns[OPTION_YES] = "Accept";
		trace_btns[OPTION_CANCEL] = "Cancel";
		trace_cyc = new JTextField();
		trace_cyc.setPreferredSize(new Dimension(200, 20));
		trace_cyc_pn = new JPanel();
		trace_cyc_pn.add(new JLabel("Cycles:"));
		trace_cyc_pn.add(trace_cyc);
		// TODO: put Low/High on same line...
		trace_lo = new JTextField();
		trace_lo.setPreferredSize(new Dimension(200, 20));
		trace_lo_pn = new JPanel();
		trace_lo_pn.add(new JLabel("Low PC(hex):"));
		trace_lo_pn.add(trace_lo);
		trace_hi = new JTextField();
		trace_hi.setPreferredSize(new Dimension(200, 20));
		trace_hi_pn = new JPanel();
		trace_hi_pn.add(new JLabel("High PC(excl):"));
		trace_hi_pn.add(trace_hi);
		trace_sec = new JTextField();
		trace_sec.setPreferredSize(new Dimension(200, 20));
		trace_sec_pn = new JPanel();
		trace_sec_pn.add(new JLabel("Seconds:"));
		trace_sec_pn.add(trace_sec);
		trace_pn.add(trace_cyc_pn);
		trace_pn.add(trace_lo_pn);
		trace_pn.add(trace_hi_pn);
		trace_pn.add(trace_sec_pn);

		// Dialog for dump page...
		dmppg_pn = new JPanel();
		dmppg_pn.setLayout(new BoxLayout(dmppg_pn, BoxLayout.Y_AXIS));
		dump_bnk = new JTextField();
		dump_bnk.setPreferredSize(new Dimension(30, 20));
		dump_bnk_pn = new JPanel();
		dump_bnk_pn.add(new JLabel("Bank (0-15):"));
		dump_bnk_pn.add(dump_bnk);
		dmppg_pn.add(dump_bnk_pn);
		dump_pg = new JTextField();
		dump_pg.setPreferredSize(new Dimension(50, 20));
		dump_pg_pn = new JPanel();
		dump_pg_pn.add(new JLabel("Page (00-FF):"));
		dump_pg_pn.add(dump_pg);
		dmppg_pn.add(dump_pg_pn);
		dump_rom = new JCheckBox("ROM Enabled");
		// For some reason, BoxLayout does like "un-wrapped" objects...
		JPanel pn = new JPanel();
		pn.add(dump_rom);
		dmppg_pn.add(pn);

		// must be done before conditional returns below...
		Thread th = new Thread(this);
		th.start();
	}

	public void setCommander(Commander cmdr) {
		_cmdr = cmdr;
		// now initialize some more menus.
		if (_main != null) {
			setupDeviceDumps();
		}
	}

	public void setupDeviceDumps() {
		JMenuItem mi;

		Vector<String> r = _cmdr.sendCommand("getdevs");
		if (!r.get(0).equals("ok")) {
			error(_main, "Failed to list drives", r.get(0));
		} else for (int x = 1; x < r.size(); ++x) {
			String dev = r.get(x);
			int key = _key++;
			_devs.put(key, dev);
			mi = new JMenuItem("Dump " + dev, key);
			mi.setText("Dump " + dev);
			mi.addActionListener(this);
			_dbg_mu.add(mi);
		}
	}

	private void setMenuText(JMenuItem mi, String drv, String mdia) {
		int sl = mdia.lastIndexOf('/');
		if (sl >= 0) {
			mdia = mdia.substring(sl + 1);
		}
		mi.setText(drv + " [" + mdia + "]");
	}

	private String respToString(Vector<String> resp, int start, boolean html) {
		int x;
		String ret = new String();
		if (html) {
			ret += "<HTML><PRE>";
		}
		for (x = start; x < resp.size(); ++x) {
			ret += resp.get(x) + "\n";
		}
		if (html) {
			ret += "</PRE></HTML>";
		}
		return ret;
	}

	private void handleResp(String title, Vector<String> resp) {
		if (dumpToLog) {
			System.err.format("------- %s -------\n%s",
				title, respToString(resp, 1, false));
		} else {
			inform(_main, title, respToString(resp, 1, true));
			_main.requestFocus();
		}
	}

	private void doTraceDialog() {
		trace_cyc.setText("");
		trace_lo.setText("");
		trace_hi.setText("");
		trace_sec.setText("1");
		int res = JOptionPane.showOptionDialog(_main, trace_pn,
				"Trace CPU", JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE, null,
				trace_btns, trace_btns[OPTION_YES]);
		_main.requestFocus();
		if (res != OPTION_YES) {
			return;
		}
		int msecs = 0;
		String cmd = "trace ";
		if (trace_cyc.getText().length() > 0) {
			// let's hope it is numeric...
			cmd += "cycles " + trace_cyc.getText();
		} else if (trace_lo.getText().length() > 0) {
			// let's hope it is hexadecimal...
			cmd += "pc " + trace_lo.getText() +
				" " + trace_hi.getText();
		} else if (trace_sec.getText().length() > 0) {
			try {
				float s = Float.valueOf(trace_sec.getText());
				msecs = (int)Math.ceil(s * 1000);
			} catch (Exception ee) {}
			if (msecs == 0) {
				return;
			}
			cmd += "on";
		}
		Vector<String> r = _cmdr.sendCommand(cmd);
		if (!r.get(0).equals("ok")) {
			error(_main, cmd, join(r));
			return;
		}
		if (msecs == 0) {
			return;
		}
		try {
			Thread.sleep(msecs);
		} catch(Exception ee) {}
		r = _cmdr.sendCommand("trace off");
		if (!r.get(0).equals("ok")) {
			error(_main, "Trace OFF", join(r));
		}
	}

	private void doDumpPageDialog() {
		if (dump_bnk != null) {
			dump_bnk.setText("");
		}
		dump_pg.setText("");
		dump_rom.setSelected(false);
		int res = JOptionPane.showOptionDialog(_main, dmppg_pn,
				"Dump Page", JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE, null,
				trace_btns, trace_btns[OPTION_YES]);
		_main.requestFocus();
		if (res != OPTION_YES) {
			return;
		}
		String cmd = "dump page ";
		if (dump_pg.getText().length() <= 0) {
			return;
		}
		if (dump_rom.isSelected()) {
			cmd += "rom ";
		}
		if (dump_bnk != null && dump_bnk.getText().length() > 0) {
			cmd += dump_bnk.getText() + " ";
		}
		// let's hope it is numeric...
		cmd += dump_pg.getText();
		Vector<String> r = _cmdr.sendCommand(cmd);
		if (!r.get(0).equals("ok")) {
			error(_main, cmd, join(r));
		} else {
			handleResp("Dump Page " + dump_pg.getText(), r);
		}
	}

	public void actionPerformed(ActionEvent e) {
		if (!(e.getSource() instanceof JMenuItem)) {
			System.err.println("unknown event source type");
			return;
		}
		JMenuItem m = (JMenuItem)e.getSource();
		int key = m.getMnemonic();
		_cmds.add(key);
	}

	private void showAbout() {
		java.net.URL url = this.getClass().getResource("docs/About.html");
		try {
			JEditorPane about = new JEditorPane(url);
			about.setEditable(false);
			Dimension dim = new Dimension(400, 300);
			about.setPreferredSize(dim);
			JOptionPane.showMessageDialog(_main, about,
				"About: Kaypro Simulator", JOptionPane.PLAIN_MESSAGE);
		} catch (Exception ee) { }
	}

	public void run() {
		int key = 0;
		while (true) {
			try {
				key = _cmds.take();
			} catch (Exception ee) {
				break;
			}
			if (key == _reset_key) {
				Vector<String> r = _cmdr.sendCommand("reset");
				if (!r.get(0).equals("ok")) {
					error(_main, "Reset", r.get(0));
				}
				continue;
			}
			if (key == _quit_key) {
				// FEexit handles graceful shutdown of back-end.
				System.exit(0);
			}
			if (key == _tracecust_key) {
				// pop-up dialog, get parameters...
				doTraceDialog();
				continue;
			}
			if (key == _traceon_key) {
				Vector<String> r = _cmdr.sendCommand("trace on");
				if (!r.get(0).equals("ok")) {
					error(_main, "Trace ON", join(r));
				}
				continue;
			}
			if (key == _traceoff_key) {
				Vector<String> r = _cmdr.sendCommand("trace off");
				if (!r.get(0).equals("ok")) {
					error(_main, "Trace OFF", join(r));
				}
				continue;
			}
			if (key == _dump_key) {
				dumpToLog = !dumpToLog;
				if (dumpToLog) {
					_dump_mi.setText("Dump To GUI");
				} else {
					_dump_mi.setText("Dump To Log");
				}
				continue;
			}
			if (key == _cpu_key) {
				Vector<String> r = _cmdr.sendCommand("dump cpu");
				if (!r.get(0).equals("ok")) {
					error(_main, "CPU Debug", join(r));
				} else {
					handleResp("CPU Debug", r);
				}
				continue;
			}
			if (key == _mach_key) {
				Vector<String> r = _cmdr.sendCommand("dump mach");
				if (!r.get(0).equals("ok")) {
					error(_main, "Machine Debug", join(r));
				} else {
					handleResp("Machine Debug", r);
				}
				continue;
			}
			if (key == _page_key) {
				doDumpPageDialog();
				continue;
			}
			if (_devs.containsKey(key)) {
				String dev = _devs.get(key);
				Vector<String> r = _cmdr.sendCommand("dump disk " + dev);
				if (!r.get(0).equals("ok")) {
					error(_main, dev + " Debug", join(r));
				} else {
					handleResp(dev + " Debug", r);
				}
				continue;
			}
			if (key == KeyEvent.VK_A) {
				showAbout();
				continue;
			}
			if (key == KeyEvent.VK_H) {
				_help.setVisible(true);
				continue;
			}
			System.err.println("unknown action key");
		}
	}

	private String join(Vector<String> vec) {
		if (vec.size() < 1) {
			return "";
		}
		String s = vec.get(0);
		for (int i = 1; i < vec.size(); ++i) {
			s += ' ' + vec.get(i);
		}
		return s;
	}

	static public void error(JFrame main, String op, String err) {
                JOptionPane.showMessageDialog(main,
                        new JLabel(err),
                        op + " Information", JOptionPane.ERROR_MESSAGE);
        }

	static public void inform(JFrame main, String op, String err) {
                JOptionPane.showMessageDialog(main,
                        new JLabel(err),
                        op + " Information", JOptionPane.INFORMATION_MESSAGE);
        }

	public void resetPerformed() {
		_cmds.add(_reset_key);
	}
}
