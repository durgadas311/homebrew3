// Copyright (c) 2022 Douglas Miller <durgadas311@gmail.com>

import javax.swing.AbstractButton;

public interface SwitchProvider {
	static final int TOGGLE0 = 1;
	static final int TOGGLE1 = 2;
	static final int TOGGLE2 = 3;
	static final int TOGGLE3 = 4;
	static final int PB0 = 10;
	static final int PB1 = 11;

	void setSwitch(int sw, AbstractButton ab);
}
