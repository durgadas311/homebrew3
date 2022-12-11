// Copyright (c) 2018 Douglas Miller <durgadas311@gmail.com>

import java.util.Arrays;
import java.util.Properties;
import java.util.Vector;
import java.awt.event.*;
import java.io.*;

public class CompactFlash implements IODevice, GenericDiskDrive,
			DiskController, ActionListener {

	static final int adr_Data_c = 0;
	static final int adr_Error_c = 1;
	static final int adr_Feature_c = 1;
	static final int adr_SecCnt_c = 2;
	static final int adr_Sector_c = 3;
	static final int adr_CylLo_c = 4;
	static final int adr_CylHi_c = 5;
	static final int adr_Head_c = 6;
	static final int adr_Status_c = 7;
	static final int adr_Cmd_c = 7;

	public static final int sts_Busy_c = 0x80;
	public static final int sts_Ready_c = 0x40;
	public static final int sts_WriteFault_c = 0x20;
	public static final int sts_DSC_c = 0x10;
	public static final int sts_Drq_c = 0x08;
	public static final int sts_Corr_c = 0x04;
	public static final int sts_Error_c = 0x01;

	// CF Command codes
	static final byte cmd_Read_c = (byte)0x20;
	static final byte cmd_Write_c = (byte)0x30;
	static final byte cmd_Features_c = (byte)0xef;

	// State Machine
	// Read:
	//	cmd_Read_c	sts_Busy_c
	//			!sts_Busy_c, sts_Drq_c
	//	(read data)	...
	//			!sts_Drq_c
	//
	// Write:
	//	cmd_Write_c	sts_Busy_c
	//			sts_Drq_c
	//	(write data)	...
	//			!sts_Busy_c, !sts_Drq_c

	private int base;
	private enum State {
		IDLE,
		COMMAND,
		DATA_IN,
		DATA_OUT,
		SENSE,
		DRIVECB,
		STATUS,
	};
	private State curState;

	private RandomAccessFile driveFd;
	private Interruptor intr;
	private LED leds_m;
	private javax.swing.Timer timer;
	private int driveSecLen;
	private int sectorsPerTrack;
	private long capacity;
	private int driveCode;
	private int driveCnum;
	private String driveMedia;
	private int driveType;
	private long mediaSpt;
	private long mediaSsz;
	private long mediaCyl;
	private long mediaHead;
	private long mediaLat;

	// mode COMMAND
	private byte[] cmdBuf = new byte[8];
	private byte curCmd;
	private byte feat;
	// mode DATA_IN/DATA_OUT
	private byte[] dataBuf;
	private int dataLength;
	private int dataIx;
	private long wrOff;

	private String name;

	CompactFlash(Properties props, LEDHandler lh, int base,
			Interruptor intr) {
		this.intr = intr;
		this.base = base;
		
		driveFd = null;
		driveSecLen = 0;
		sectorsPerTrack = 0;
		capacity = 0;
		driveCode = 0;
		driveCnum = 0;
		driveMedia = null;
		driveType = 0;
		dataBuf = null;
		dataLength = 0;
		dataIx = 0;

		int sectorSize = 512;

		timer = new javax.swing.Timer(500, this);
		name = "CF";
		driveMedia = name;
		String s = props.getProperty("cf_drive");
		if (s != null) {
			String[] ss = s.split("\\s", 2);
			if (ss[0].matches("[0-9]+[MmGg]")) {
				int i = ss[0].length() - 1;
				char f = Character.toUpperCase(ss[0].charAt(i));
				capacity = Integer.valueOf(ss[0].substring(0, i)) *
					1024 * 1024;	// Megabytes at least...
				if (f == 'G') {
					capacity *= 1024;
				}
			}
			if (ss.length > 1) {
				// Front panel name, not media name
				name = ss[1];
			}
		} else {
			capacity = 64*1024*1024;
		}
		s = props.getProperty("cf_disk");
		if (s != null) {
			// TODO: any parameters follow?
			driveMedia = s;
		}
		// Always show drive on front panel, even if not usable
		leds_m = lh.registerLED(name);
		driveSecLen = 512;

		dataBuf = new byte[sectorSize + 4];	// space for ECC for "long" commands
		dataLength = driveSecLen;

		RandomAccessFile fd;
		try {
			fd = new RandomAccessFile(driveMedia, "rw");
			if (fd.length() == 0) {
				// special case: new media - initialize it.
				System.err.format("Initializing new media %s as %dM\n", driveMedia, capacity / 1024 / 1024);
				fd.setLength(capacity);
			} else {
				capacity = fd.length();
				System.err.format("Mounted existing media %s as %dM\n", driveMedia, capacity / 1024 / 1024);
			}
		} catch (Exception ee) {
			System.err.format("WD1002_05: Unable to open media %s\n", driveMedia);
			return;
		}
		driveFd = fd;
		if (driveFd != null) {
			cmdBuf[adr_Status_c] |= sts_Ready_c;
		} else {
			cmdBuf[adr_Status_c] &= ~sts_Ready_c;
		}
	}

	private int getCyl() {
		return ((cmdBuf[adr_CylHi_c] & 0xff) << 8) | (cmdBuf[adr_CylLo_c] & 0xff);
	}

	private int getSec() {
		return cmdBuf[adr_Sector_c] & 0xff;
	}

	private int getCount() {
		return cmdBuf[adr_SecCnt_c] & 0xff;
	}

	private int getHead() {
		return cmdBuf[adr_Head_c] & 0x07;
	}

	private int getLUN(byte b) {
		return (b & 0x18) >> 3;
	}

	private int getLUN() {
		return getLUN(cmdBuf[adr_Head_c]);
	}

	private int getSSZ() {
		return 512;
	}

	private long getOff() {
		// TODO: support non-LBA mode...
		long off = (((cmdBuf[adr_Head_c] & 0x0f) << 24) |
			((cmdBuf[adr_CylHi_c] & 0xff) << 16) |
			((cmdBuf[adr_CylLo_c] & 0xff) << 8) |
			(cmdBuf[adr_Sector_c] & 0xff));
		off *= driveSecLen;
		return off;
	}

	public GenericDiskDrive findDrive(String name) {
		if (name.equals(this.name)) {
			return this;
		}
		return null;
	}
	public Vector<GenericDiskDrive> getDiskDrives() {
		// Only return REMOVABLE drives...
		Vector<GenericDiskDrive> drives = new Vector<GenericDiskDrive>();
		//drives.add(this);
		return drives;
	}

	public int getBaseAddress() { return base; }
	public int getNumPorts() { return 8; }

	public int in(int port) {
		port &= 7;
		int val = cmdBuf[port] & 0xff;
		switch(port) {
		case adr_Data_c:
			getData();
			break;
		case adr_Error_c:
			// TODO: reset bits?
			break;
		case adr_Status_c:
			// TODO: reset bits?
			cmdBuf[adr_Status_c] &= ~sts_Error_c;
			break;
		}
		return val;
	}

	public void out(int port, int val) {
		port &= 7;
		switch(port) {
		case adr_Feature_c:
			// anything?
			feat = (byte)val;
			return;
		case adr_Cmd_c:
			//cmdBuf[adr_Status_c] |= sts_Busy_c;
			curCmd = (byte)val;
			processCmd();
			return;
		case adr_Data_c:
			putData(val);
			break;
		case adr_Head_c:
			if (driveFd != null) {
				cmdBuf[adr_Status_c] |= sts_Ready_c;
			} else {
				cmdBuf[adr_Status_c] &= ~sts_Ready_c;
			}
			break;
		}
		cmdBuf[port] = (byte)val;
	}

	public void reset() {
		dataIx = 0;
		Arrays.fill(cmdBuf, (byte)0);
		curCmd = 0;
		if (driveFd != null) {
			cmdBuf[adr_Status_c] |= sts_Ready_c;
			//cmdBuf[adr_Status_c] |= sts_SeekDone_c;
		}
	}

	private void putData(int val) {
		if (dataIx < dataLength) {
			dataBuf[dataIx++] = (byte)val;
			if (dataIx < dataLength) {
				cmdBuf[adr_Status_c] |= sts_Drq_c;
			} else {
				processData();
			}
			return;
		}
		setDone();
	}

	private void getData() {
		if (dataIx < dataLength) {
			cmdBuf[adr_Data_c] = dataBuf[dataIx++];
			cmdBuf[adr_Status_c] |= sts_Drq_c;
			return;
		}
		dataIx = 0;
		if (cmdBuf[adr_SecCnt_c] > 0) {
			--cmdBuf[adr_SecCnt_c];
			++cmdBuf[adr_Sector_c];
			// TODO: carry over to cylinder? head?
		}
		if (cmdBuf[adr_SecCnt_c] > 0) {
			processCmd();
			return;
		}
		setDone();
	}

	private void setDone() {
		cmdBuf[adr_Status_c] &= ~sts_Drq_c;
		cmdBuf[adr_Status_c] &= ~sts_Busy_c;
	}

	private void setError() {
		cmdBuf[adr_Status_c] |= sts_Error_c;
		setDone();
	}

	private void dumpIO(String op, long off) {
		System.err.format("%s at %d (%d %d %d %d):",
			op, off, getLUN(), getCyl(), getHead(), getSec());
		for (int x = 0; x < 16; ++x) {
			System.err.format(" %02x", dataBuf[x]);
		}
		System.err.format("\n");
	}

	private void processCmd() {
		long off;
		long e;

		if (driveFd == null) {
			setError();
			return;
		}
		timer.removeActionListener(this);
		timer.addActionListener(this);
		timer.restart();
		// TODO: minimize redundant calls?
		leds_m.set(true);
		switch (curCmd & 0xf0) {
		case cmd_Read_c:
			off = getOff();
			if (off >= capacity) {
				setError();
				break;
			}
			try {
				driveFd.seek(off);
				// dataBuf includes (fake) ECC, must limit read()...
				e = driveFd.read(dataBuf, 0, driveSecLen);
				if (e != driveSecLen) {
					setError();
					break;
				}
			} catch (Exception ee) {
				setError();
				break;
			}
			dataLength = driveSecLen;
			dataIx = 0;
			getData(); // prime first byte
			break;
		case cmd_Write_c:
			// Prepare for command... but must wait for data...
			wrOff = getOff();
			if (wrOff >= capacity) {
				setError();
				break;
			}
			dataLength = driveSecLen;
// TODO: support other commands?
//			if ((curCmd & cmd_Long_c) != 0) {
//				dataLength += 4;
//			}
			dataIx = 0;
			cmdBuf[adr_Status_c] |= sts_Drq_c;
			cmdBuf[adr_Status_c] &= ~sts_Busy_c;
			break;
		default:
			break;
		}

	}

	private void processData() {
		long off;
		long e;

		switch (curCmd & 0xf0) {
		case cmd_Write_c:
			//off = getOff();
			off = wrOff;
			try {
				driveFd.seek(off);
				// dataBuf includes (fake) ECC, must limit write()...
				driveFd.write(dataBuf, 0, driveSecLen);
			} catch (Exception ee) {
				setError();
				break;
			}
			dataIx = 0;
			if (cmdBuf[adr_SecCnt_c] > 0) {
				--cmdBuf[adr_SecCnt_c];
				++cmdBuf[adr_Sector_c];
				// TODO: carry over to cylinder? head?
			}
			if (cmdBuf[adr_SecCnt_c] > 0) {
				cmdBuf[adr_Status_c] |= sts_Drq_c;
				cmdBuf[adr_Status_c] |= sts_Busy_c;
				break;
			}
			setDone();
			break;
		default:
			break;
		}
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() != timer) {
			return;
		}
		timer.removeActionListener(this);
		leds_m.set(false);
	}

	public String getMediaName() {
		return driveMedia != null ? driveMedia : "";
	}

	public boolean isReady() {
		return driveFd != null;
	}

	public void insertDisk(GenericFloppyDisk disk) { }
	public int getRawBytesPerTrack() { return 0; }
	public int getNumTracks() { return 0; }
	public int getNumHeads() { return 0; } 
	public int getMediaSize() { return 0; }
	public boolean isRemovable() { return false; }

	public String getDriveName() {
		return name;
	}

	public String getDeviceName() { return "CF"; }

	public String dumpDebug() {
		String ret = String.format(
			"[0] data    %02x\n" +
			"[1] error   %02x %02x (feature)\n" +
			"[2] sec cnt %02x\n" +
			"[3] sector  %02x\n" +
			"[4] cyl lo  %02x\n" +
			"[5] cyl hi  %02x\n" +
			"[6] S/D/H   %02x\n" +
			"[7] status  %02x %02x (cmd)\n" +
			"data index = %d\n",
			cmdBuf[0] & 0xff,
			cmdBuf[1] & 0xff, feat & 0xff,
			cmdBuf[2] & 0xff,
			cmdBuf[3] & 0xff,
			cmdBuf[4] & 0xff,
			cmdBuf[5] & 0xff,
			cmdBuf[6] & 0xff,
			cmdBuf[7] & 0xff, curCmd & 0xff,
			dataIx);
		return ret;
	}
}
