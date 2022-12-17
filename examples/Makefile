# Uses 'zmac' to assemble programs. See http://48k.ca/zmac.html

all: example1.hex example2.hex

%.cim: %.asm
	zmac --dri -i -8 -c -s -n --od . --oo cim,lst $?

%.hex: %.asm
	zmac --dri -i -8 -c -s -n --od . --oo hex,lst $?
