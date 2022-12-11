// Copyright (c) 2017 Douglas Miller <durgadas311@gmail.com>

import javax.swing.JMenuItem;

public interface LEDHandler {
	LED registerLED(String drive);
	LED[] registerLEDs(String drive, int num, LED.Colors[] colors);
	void setMenuItem(String drive, JMenuItem mi);
}
