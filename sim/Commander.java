// Copyright 2021 Douglas Miller <durgadas311@gmail.com>
import java.util.Vector;

public interface Commander {
	Vector<String> sendCommand(String cmd);
	void setSwitches(int sw, boolean on);
}
