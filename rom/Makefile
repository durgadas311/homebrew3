all: rom3-0.cim rom3-l.cim

%.cim: %.asm
	zmac --dri -i -8 -c -s -n --od . --oo cim,lst $?

%.hex: %.asm
	zmac --dri -i -8 -c -s -n --od . --oo hex,lst $?
