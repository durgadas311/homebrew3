// Copyright (c) 2017 Douglas Miller <durgadas311@gmail.com>

public interface Interruptor {
	int registerINT(int irq);
	void raiseINT(int irq, int src);
	void lowerINT(int irq, int src);
	void blockInts(int mask);
	void unblockInts(int mask);
	void setNMI(boolean state);	// As implemented by Kaypro
	void triggerNMI();		// Not used/available for Kaypro
	void addClockListener(ClockListener lstn);
	void addIntrController(InterruptController ctrl);
	void waitCPU();
	boolean isTracing();
	void startTracing();
	void stopTracing();
}
