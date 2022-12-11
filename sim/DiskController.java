// Copyright (c) 2017 Douglas Miller <durgadas311@gmail.com>

import java.util.Vector;

public interface DiskController extends IODevice {
	GenericDiskDrive findDrive(String name);
	Vector<GenericDiskDrive> getDiskDrives();
}
