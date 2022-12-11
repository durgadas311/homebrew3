// Copyright (c) 2017 Douglas Miller <durgadas311@gmail.com>

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

class LEDPane extends JPanel implements MouseListener {
	JMenuItem mnu = null;
	String name;

	public LEDPane(Color bg) {
		super();
		setOpaque(false);
		setBackground(bg);
		addMouseListener(this);
	}

	public void setMenuItem(JMenuItem mi) {
		mnu = mi;
	}

	public void setName(String nm) {
		name = nm;
	}

	public String getName() { return name; }

	public void mouseClicked(MouseEvent e) {
		if (e.getButton() != MouseEvent.BUTTON1) {
			return;
		}
		if (mnu == null) {
			return;
		}
		mnu.doClick();
	}
	public void mouseEntered(MouseEvent e) {
		if (mnu == null) {
			return;
		}
		setOpaque(true);
		repaint();
	}
	public void mouseExited(MouseEvent e) {
		if (mnu == null) {
			return;
		}
		setOpaque(false);
		repaint();
	}
	public void mousePressed(MouseEvent e) { }
	public void mouseReleased(MouseEvent e) { }
}
