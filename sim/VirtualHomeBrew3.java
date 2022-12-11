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
				File f = new File(args[x]);
				if (f.exists()) {
					config = f.getAbsolutePath();
				}
			}
		}
		if (config == null) {
			config = System.getenv("HB3_CONFIG");
			if (config == null) {
				config = "vhb3";
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
		if (props.getProperty("gui") != null) {
			gui = true;
		}
		String title = "Virtual HomeBrew3 Computer";
		HDSP_2111 dsp = null;

		if (gui) {
			front_end = new JFrame(title);
			front_end.getContentPane().setName("HomeBrew3 Emulator");
			front_end.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			front_end.getContentPane().setBackground(new Color(25, 25, 25));
			// This allows TAB to be sent
			//front_end.setFocusTraversalKeysEnabled(false);
			dsp = new HDSP_2111(props, 0x80);
		}
		HB3FrontSide zmc = new HB3FrontSide(front_end, props, dsp);
		JPanel pn = zmc;

		HomeBrew3 sys = new HomeBrew3(props);
		if (dsp != null) {
			sys.addDevice(dsp);
		}
		// All LEDs should be registered now...
		HB3Operator op = new HB3Operator(front_end, props);
		op.setCommander(sys.getCommander());
		zmc.reportSwitches(op);

		if (gui) {
			front_end.add(pn);
			front_end.pack();
			front_end.setVisible(true);
		}
		sys.start(); // spawns its own thread... returns immediately
		//front_end.toBack(); // not really what we want...
	}
}
