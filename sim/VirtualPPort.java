// Copyright (c) 2017 Douglas Miller <durgadas311@gmail.com>

public interface VirtualPPort {
	int available();	// Num bytes available from Port
	int take(boolean sleep); // Get byte from Port (allow sleep)
	boolean ready();	// Port accepting input
	void put(int ch, boolean sleep);	// Send byte to port input
	boolean attach(Object periph);
	void detach();		// Peripheral no longer usable
}
