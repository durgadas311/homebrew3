// Copyright (c) 2017 Douglas Miller <durgadas311@gmail.com>

public interface InterruptController {
	int readDataBus(); // return -1 if no interrupt pending (for this device).
	boolean retIntr();	// notifcation that CPU executed RETI (not RETN).
				// returns 'true' if this was the interrupting device.
}
