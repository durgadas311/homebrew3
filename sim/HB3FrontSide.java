// Copyright (c) 2018 Douglas Miller
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.Properties;
import java.util.Vector;
import java.util.Map;
import java.util.HashMap;
import javax.swing.border.*;

class LEDPanel extends JPanel {
	LEDPane[] panes;
	public static final Font font = new Font("Monospaced", Font.BOLD, 16);

	public LEDPanel(int w, int h, int rows, Color bg) {
		super();
		panes = new LEDPane[rows];
		GridBagLayout gb = new GridBagLayout();
		GridBagConstraints gc = new GridBagConstraints();
		gc.fill = GridBagConstraints.NONE;
		gc.gridx = 0;
		gc.gridy = 0;
		gc.weightx = 0;
		gc.weighty = 0;
		gc.gridwidth = 1;
		gc.gridheight = 1;
		setLayout(gb);
		setOpaque(false);
		setPreferredSize(new Dimension(w, h));
		for (int y = 0; y < rows; ++y) {
			LEDPane pn = new LEDPane(bg);
			pn.setPreferredSize(new Dimension(w, h / rows));
			gb.setConstraints(pn, gc);
			add(pn);
			panes[y] = pn;
			++gc.gridy;
		}
	}

	public LEDPane getPane(int row) {
		if (row < panes.length) {
			return panes[row]; // should not be null
		} else {
			return null;
		}
	}

	public void putMap(String drive, JMenuItem mi) {
		for (LEDPane pn : panes) {
			if (pn == null) continue;
			if (drive.equals(pn.getName())) {
				pn.setMenuItem(mi);
			}
		}
	}

	public void reFmt(String fmt) {
		for (LEDPane pn : panes) {
			if (pn == null) continue;
			// Component #0 is always the label...
			// But, it might not be there yet...
			int n = pn.getComponentCount();
			if (n < 1) {
				continue;
			}
			JLabel lb = (JLabel)pn.getComponent(0);
			lb.setText(String.format(fmt, lb.getText()));
		}
	}

	public void rePad(int num) {
		++num; // one component is the label...
		for (LEDPane pn : panes) {
			if (pn == null) continue;
			int n = pn.getComponentCount();
			if (n <= 0 || n >= num) continue;
			for (int x = n; x < num; ++x) {
				JPanel p = new JPanel();
				p.setPreferredSize(RectLED.getDim());
				pn.add(p);
			}
		}
	}
}

public class HB3FrontSide extends JPanel
		implements LEDHandler {
	static final long serialVersionUID = 198900000004L;

	LEDPanel _ledspace;
	int _ledy;
	int _ledmax = 1;
	int _ledwid = 5;
	String _ledfmt = "%5s";
	int gaps;
	float rounding;
	int width, height;
	int offset;

	public HB3FrontSide(JFrame main, Properties props) {
		super();
		if (props == null) {}
		if (main == null) {
			return;
		}
		setBackground(main.getContentPane().getBackground());
		setOpaque(true);
		Dimension dim = new Dimension(400, 300);
		gaps = dim.width / 50;
		if (gaps < 5) gaps = 5;
		offset = dim.width / 40;
		rounding = offset * 2;
		width = Math.round(dim.width + 2 * offset);
		height = Math.round(dim.height + 2 * offset);

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
		pan.setPreferredSize(new Dimension(5, 5));
		pan.setOpaque(false);
		gc.gridx = 0;
		gc.gridwidth = 3;
		gridbag.setConstraints(pan, gc);
		gc.gridwidth = 1;
		add(pan);
		++gc.gridy;

		pan = new JPanel();
		pan.setPreferredSize(new Dimension(5, 5));
		pan.setOpaque(false);
		gc.gridx = 0;
		gridbag.setConstraints(pan, gc);
		add(pan);

		_ledspace = new LEDPanel(dim.width - 20, dim.height, 4,
					getBackground().brighter().brighter());
		_ledy = 0;
		gc.gridx = 1;
		gridbag.setConstraints(_ledspace, gc);
		add(_ledspace);

		pan = new JPanel();
		pan.setPreferredSize(new Dimension(5, 5));
		pan.setOpaque(false);
		gc.gridx = 2;
		gridbag.setConstraints(pan, gc);
		add(pan);

		++gc.gridy;
		gc.gridx = 0;

		pan = new JPanel();
		pan.setPreferredSize(new Dimension(5, 5));
		pan.setOpaque(false);
		gc.gridwidth = 3;
		gridbag.setConstraints(pan, gc);
		gc.gridwidth = 1;
		add(pan);
	}

	public void setMenuItem(String drive, JMenuItem mi) {
		_ledspace.putMap(drive, mi);
	}

	public LED registerLED(String drive, LED.Colors color) {
		LED led[] = registerLEDs(drive, 1, new LED.Colors[]{color});
		return led[0];
	}

	public LED registerLED(String drive) {
		return registerLED(drive, LED.Colors.RED);
	}

	public LED[] registerLEDs(String drive, int num, LED.Colors[] colors) {
		LED[] leds = new LED[num];
		LEDPane pn = null;
		if (_ledspace != null) {
			if (_ledmax < num) {
				_ledmax = num;
				_ledspace.rePad(num);
			}
			if (drive.length() > 20) {
				drive = drive.substring(0, 20);
			}
			if (drive.length() > _ledwid) {
				_ledwid = drive.length();
				_ledfmt = String.format("%%%ds", _ledwid);
				_ledspace.reFmt(_ledfmt);
			}
			pn = _ledspace.getPane(_ledy++);
		}
		if (pn != null) {
			pn.setName(drive);
			JLabel lb = new JLabel(String.format(_ledfmt, drive));
			lb.setForeground(Color.white);
			lb.setFont(LEDPanel.font);
			pn.add(lb);
		}
		for (int x = 0; x < num; ++x) {
			leds[x] = new RectLED(colors[x]);
			if (pn != null) {
				pn.add(leds[x]);
			}
		}
		if (pn != null) {
			for (int x = num; x < _ledmax; ++x) {
				JPanel p = new JPanel();
				p.setPreferredSize(RectLED.getDim());
				pn.add(p);
			}
		}
		return leds;
	}
}
