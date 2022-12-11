// Copyright (c) 2018 Douglas Miller
import java.awt.*;
import javax.swing.*;
import java.io.*;
import java.util.Properties;
import java.lang.reflect.Field;
import java.util.Properties;

public class VirtualHomeBrew3 {
	private static JFrame front_end;

	public static void main(String[] args) {
		Properties props = new Properties();
		String config = null;
		boolean gui = false;
		for (int x = 0; x < args.length; ++x) {
			if (args[x].equals("gui")) {
				gui = true;
			} else {
				File f = new File(args[0]);
				if (f.exists()) {
					config = f.getAbsolutePath();
				}
			}
		}
		if (config == null) {
			config = System.getenv("Z80MC_CONFIG");
			if (config == null) {
				config = "vz80mc";
				File f = new File(config);
				if (f.exists()) {
					config = f.getAbsolutePath();
				} else {
					config = System.getProperty("user.home") + "/." + config;
				}
			}
		}
		if (config != null) {
			try {
				FileInputStream cfg = new FileInputStream(config);
				props.setProperty("configuration", config);
				props.load(cfg);
				cfg.close();
			} catch(Exception ee) {
				config = null;
			}
		}
		String title = "Virtual Z80-minicomp Computer";

		if (gui) {
			front_end = new JFrame(title);
			front_end.getContentPane().setName("Z80-minicomp Emulator");
			front_end.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			front_end.getContentPane().setBackground(new Color(25, 25, 25));
			// This allows TAB to be sent
			//front_end.setFocusTraversalKeysEnabled(false);
		}
		Z80mcFrontSide zmc = new Z80mcFrontSide(front_end, props);
		JPanel pn = zmc;
		LEDHandler lh = zmc;

		HomeBrew3 sys = new HomeBrew3(props, lh);
		// All LEDs should be registered now...
		Z80mcOperator op = new Z80mcOperator(front_end, props, lh);
		op.setCommander(sys.getCommander());

		if (gui) {
			front_end.add(pn);
			front_end.pack();
			front_end.setVisible(true);
		}
		sys.start(); // spawns its own thread... returns immediately
		//front_end.toBack(); // not really what we want...
	}
}
