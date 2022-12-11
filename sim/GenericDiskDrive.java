// Copyright (c) 2016 Douglas Miller <durgadas311@gmail.com>

public interface GenericDiskDrive {
	String getDriveName();
	String getMediaName();
	void insertDisk(GenericFloppyDisk dsk);
	int getRawBytesPerTrack();
	int getNumTracks();
	int getNumHeads();
	int getMediaSize();
	boolean isRemovable();
	boolean isReady();
}
