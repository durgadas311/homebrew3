// Copyright (c) 2018 Douglas Miller <durgadas311@gmail.com>

public interface TimerCounterChannel extends TimerCounter {
	void attach(TimerCounter tcr);
	void attach(BaudListener bdl);
}
