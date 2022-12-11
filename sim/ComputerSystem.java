// Copyright 2018 Douglas Miller <durgadas311@gmail.com>

public interface ComputerSystem {
	void reset();
	// These suspend the CPU while processing.
	void dmaRead(byte[] buf, int adr, int len);
	void dmaWrite(byte[] buf, int adr, int len);
	// TODO: interfaces for I/O ports?
	// TODO: interfaces for adding devices, ... ?
}
