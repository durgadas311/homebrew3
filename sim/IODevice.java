// Copyright (c) 2017 Douglas Miller <durgadas311@gmail.com>

public interface IODevice {
	void reset();
	int getBaseAddress();
	int getNumPorts();
	int in(int port);
	void out(int port, int value);
	String getDeviceName();
	String dumpDebug();
}
