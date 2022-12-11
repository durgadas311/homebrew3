// Copyright 2021 Douglas Miller <durgadas311@gmail.com>

import java.util.Properties;
import java.util.Vector;
import java.io.*;

public class StdioDebugger implements Runnable {
	Commander cmd;
	BufferedReader inp;

	public StdioDebugger(Properties props, Commander cmd) {
		this.cmd = cmd;
		try {
			inp = new BufferedReader(new InputStreamReader(System.in));
		} catch (Exception ee) {
			ee.printStackTrace();
			return;
		}
		Thread t = new Thread(this);
		t.start();
	}

	private String respToString(Vector<String> resp, int start, boolean html) {
		int x;
		String ret = new String();
		for (x = start; x < resp.size(); ++x) {
			ret += resp.get(x) + "\n";
		}
		return ret;
	}

	private void handleResp(String title, Vector<String> resp) {
		System.err.format("------- %s -------\n%s",
			title, respToString(resp, 1, false));
	}

	public void run() {
		String line;
		System.err.format("StdioDebugger active\n");
		while (true) {
			try {
				line = inp.readLine();
			} catch(Exception ee) {
				// TODO: silent or not?
				break;
			}
			Vector<String> ret = cmd.sendCommand(line);
			if (!ret.get(0).equals("ok")) {
				for (String s : ret) {
					System.err.format("%s ", s);
				}
				System.err.format("\n");
			} else {
				handleResp(line, ret);
			}
		}
	}
}
