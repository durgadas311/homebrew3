// Copyright (c) 2017 Douglas Miller <durgadas311@gmail.com>

public interface GenericFloppyDisk {
	boolean checkWriteProtect();
	int densityFactor();	// 1, 2, 3, ...
	int hardSectored();
	int mediaSize();
	long trackLen();
	int readData(int track, int side, int sector, int inSector);
	int writeData(int track, int side, int sector, int inSector, int data,
		boolean dataReady);
	boolean isReady();
	void eject(String name);
	String getMediaName();
}
