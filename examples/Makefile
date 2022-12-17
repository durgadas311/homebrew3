all: example1.hex

%.cim: %.asm
	zmac --dri -i -8 -c -s -n --od . --oo cim,lst $?

%.hex: %.asm
	zmac --dri -i -8 -c -s -n --od . --oo hex,lst $?
