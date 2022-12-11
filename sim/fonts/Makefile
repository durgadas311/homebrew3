all: hdsp-2111.bin

hdsp-2111.bin: hdsp-2111.o
	objcopy -j .data -O binary $? $@
