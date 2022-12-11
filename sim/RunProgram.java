// Copyright (c) 2011,2017 Douglas Miller

import java.io.*;
import java.util.Arrays;
import java.util.Vector;

public class RunProgram implements Runnable {
	private static boolean _cygwin;
	private static String[] _shell;
	private static boolean _windows;

	public Process proc = null;
	public Exception excp = null;
	private String[] cmd;

	private InputStream stdin;

	static {
		_cygwin = false;
		_windows = (System.getProperty("os.name").indexOf("Windows") >= 0);
		if (_windows) {
			File shell = new File("c:\\cygwin64\\bin\\bash.exe");
			if (!shell.exists()) {
				shell = new File("c:\\cygwin32\\bin\\bash.exe");
			}
			if (!shell.exists()) {
				shell = new File("c:\\cygwin\\bin\\bash.exe");
			}
			if (shell.exists()) {
				_shell = new String[]{ shell.getAbsolutePath(),
						"--login", "-i", "-c" };
				_cygwin = true;
				_windows = false; // for all intents and purposes?
			} else {
				_shell = new String[]{ "cmd.exe", "/c" };
			}
		} else {
			String sh = System.getenv("SHELL");
			if (sh == null) {
				// what else to do?
				_shell = new String[]{ "sh", "-c" };
			} else {
				_shell = new String[]{ sh, "-c" };
			}
		}
	}

	public static boolean isWindows() { return _windows; }

	// start command from shell and return handle
	public RunProgram(String cmd, InputStream in, boolean merge) {
		stdin = in;
		try {
			this.cmd = Arrays.copyOf(_shell, _shell.length + 1);
			this.cmd[_shell.length] = cmd;
			ProcessBuilder pcmd = new ProcessBuilder(this.cmd);
			pcmd.redirectErrorStream(merge);
			proc = pcmd.start();
			Thread t = new Thread(this);
			t.start();
		} catch(Exception ee) {
			excp = ee;
		}
	}

	// setup command and return handle
	public RunProgram(Vector<String> cmd, InputStream in) {
		stdin = in;
		this.cmd = cmd.toArray(new String[0]);
		// Start listening for modem control lines
		Thread t = new Thread(this);
		t.start();
	}

	// start command and return handle
	public RunProgram(Vector<String> cmd, InputStream in, boolean merge) {
		stdin = in;
		this.cmd = cmd.toArray(new String[0]);
		try {
			ProcessBuilder pcmd = new ProcessBuilder(this.cmd);
			pcmd.redirectErrorStream(merge);
			proc = pcmd.start();
			Thread t = new Thread(this);
			t.start();
		} catch(Exception ee) {
			excp = ee;
		}
	}

	public void start(boolean merge) {
		if (proc != null) {
			return;
		}
		try {
			ProcessBuilder pcmd = new ProcessBuilder(this.cmd);
			pcmd.redirectErrorStream(merge);
			proc = pcmd.start();
			// Thread started earlier... never dies...
		} catch(Exception ee) {
			excp = ee;
		}
	}
	public void stop() {
		if (proc == null) {
			return;
		}
	}

	// This thread reads the "UART" and sends to program stdin
	public void run() {
		while (true) {
			try {
				int c = stdin.read();
				proc.getOutputStream().write(c);
				proc.getOutputStream().flush();
			} catch (Exception ee) {
				ee.printStackTrace();
				try {
					stdin.close();
				} catch (Exception eee) {}
				break;
			}
		}
		// TODO: terminate and detach...
	}
}
