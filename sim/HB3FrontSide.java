// Copyright (c) 2018 Douglas Miller
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.Properties;
import java.util.Vector;
import java.util.Map;
import java.util.HashMap;
import javax.swing.border.*;

public class HB3FrontSide extends JPanel {
	static final long serialVersionUID = 198900000004L;

	Color paper = new Color(255, 255, 220);
	Color ink = new Color(100, 100, 0);
	Color phenolic = new Color(214, 176, 132);

	JCheckBox tg0;
	JCheckBox tg1;
	JCheckBox tg2;
	JCheckBox tg3;
	JButton pb0;
	JButton pb1;

	public HB3FrontSide(JFrame main, Properties props, JTextArea dsp) {
		super();
		if (props == null) {}
		if (main == null) {
			return;
		}
		Icon sw_w_on = new ImageIcon(this.getClass().getResource("icons/toggle_on.png"));
		Icon sw_w_off = new ImageIcon(this.getClass().getResource("icons/toggle_off.png"));
		Icon pb_r_on = new ImageIcon(this.getClass().getResource("icons/pb_on.png"));
		Icon pb_r_off = new ImageIcon(this.getClass().getResource("icons/pb_off.png"));

		setBackground(phenolic);
		setOpaque(true);

		tg0 = toggle(0, sw_w_on, sw_w_off);
		tg1 = toggle(1, sw_w_on, sw_w_off);
		tg2 = toggle(2, sw_w_on, sw_w_off);
		tg3 = toggle(3, sw_w_on, sw_w_off);
		pb0 = pushbutton(0, pb_r_on, pb_r_off);
		pb1 = pushbutton(1, pb_r_on, pb_r_off);

		String s = props.getProperty("switches");
		if (s != null) {
			int sw = Integer.valueOf(s, 2) & 0x0f;
			if ((sw & 1) != 0) {
				tg0.setSelected(true);
			}
			if ((sw & 2) != 0) {
				tg1.setSelected(true);
			}
			if ((sw & 4) != 0) {
				tg2.setSelected(true);
			}
			if ((sw & 8) != 0) {
				tg3.setSelected(true);
			}
		}

		GridBagLayout gridbag = new GridBagLayout();
		setLayout(gridbag);
		GridBagConstraints gc = new GridBagConstraints();
		gc.fill = GridBagConstraints.NONE;
		gc.gridx = 0;
		gc.gridy = 0;
		gc.weightx = 0;
		gc.weighty = 0;
		gc.gridwidth = 1;
		gc.gridheight = 1;
		gc.anchor = GridBagConstraints.NORTH;
		JPanel pan;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(10, 10));
		pan.setOpaque(false);
		gc.gridx = 0;
		gridbag.setConstraints(pan, gc);
		add(pan);
		++gc.gridy;

		pan = new JPanel();
		pan.setPreferredSize(new Dimension(10, 10));
		pan.setOpaque(false);
		gc.gridx = 6;
		gc.gridy = 7;
		gridbag.setConstraints(pan, gc);
		add(pan);

		// place-holder for switches (etc?)
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(100, 100));
		pan.setOpaque(false);
		gc.gridwidth = 2;
		gc.gridheight = 1;
		gc.gridx = 1;
		gc.gridy = 1;
		gridbag.setConstraints(pan, gc);
		add(pan);

		gc.gridwidth = 2;
		gc.gridheight = 1;
		JLabel lab = label("RESET", 50);
		gc.gridx = 1;
		gc.gridy = 2;
		gridbag.setConstraints(lab, gc);
		add(lab);
		gc.gridx += 2;
		lab = label("NMI", 30);
		gridbag.setConstraints(lab, gc);
		add(lab);

		gc.gridx = 1;
		gc.gridy = 3;
		gridbag.setConstraints(pb0, gc);
		add(pb0);
		gc.gridx += 2;
		gridbag.setConstraints(pb1, gc);
		add(pb1);

		gc.gridwidth = 1;
		gc.gridheight = 1;
		lab = label("4800", 40);
		gc.gridx = 1;
		gc.gridy = 4;
		gridbag.setConstraints(lab, gc);
		add(lab);
		gc.gridheight = 1;
		gc.gridx = 1;
		gc.gridy = 5;
		gridbag.setConstraints(tg0, gc);
		add(tg0);
		++gc.gridx;
		gridbag.setConstraints(tg1, gc);
		add(tg1);
		++gc.gridx;
		gridbag.setConstraints(tg2, gc);
		add(tg2);
		++gc.gridx;
		gridbag.setConstraints(tg3, gc);
		add(tg3);
		++gc.gridy;
		gc.gridx = 1;
		lab = label("9600", 40);
		gridbag.setConstraints(lab, gc);
		add(lab);

		if (dsp != null) {
			gc.gridx = 5;
			gc.gridy = 1;
			gridbag.setConstraints(dsp, gc);
			add(dsp);
		}
	}

	public void reportSwitches(HB3Operator op) {
		op.setSwitch(SwitchProvider.TOGGLE0, tg0);
		op.setSwitch(SwitchProvider.TOGGLE1, tg1);
		op.setSwitch(SwitchProvider.TOGGLE2, tg2);
		op.setSwitch(SwitchProvider.TOGGLE3, tg3);
		op.setSwitch(SwitchProvider.PB0, pb0);
		op.setSwitch(SwitchProvider.PB1, pb1);
	}

	private JLabel label(String txt, int wid) {
		JLabel lab = new JLabel(txt);
		lab.setPreferredSize(new Dimension(wid, 15));
		lab.setBackground(paper);
		lab.setForeground(ink);
		lab.setOpaque(true);
		return lab;
	}

	private JCheckBox toggle(int x, Icon on, Icon off) {
		JCheckBox sw = new JCheckBox();
		sw.setPreferredSize(new Dimension(50, 30));
		sw.setHorizontalAlignment(SwingConstants.CENTER);
		sw.setFocusable(false);
		sw.setFocusPainted(false);
		sw.setBorderPainted(false);
		sw.setSelectedIcon(on);
		sw.setIcon(off);
		sw.setOpaque(false);
		sw.setBackground(getBackground());
		sw.setContentAreaFilled(false);
		return sw;
	}

	private JButton pushbutton(int x, Icon on, Icon off) {
		JButton sw = new JButton();
		sw.setPreferredSize(new Dimension(50, 30));
		// addMouseListener(this);
		// addActionListener(this);
		sw.setFocusable(false);
		sw.setFocusPainted(false);
		sw.setBorderPainted(false);
		sw.setPressedIcon(on);
		sw.setIcon(off);
		sw.setOpaque(false);
		sw.setBackground(getBackground());
		sw.setContentAreaFilled(false);
		return sw;
	}
}
