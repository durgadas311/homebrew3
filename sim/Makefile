SHELL=/bin/bash
CLASS_PATH = .:z80cpu/z80core.jar

JAVAS = $(wildcard *.java)
CLASSES = $(subst .java,.class,$(JAVAS))

all: z80cpu/z80core.jar $(CLASSES)

%.class: %.java
	javac -cp $(CLASS_PATH) $?

run:
	java -cp $(CLASS_PATH) VirtualHomeBrew3

VirtualHomeBrew3.jar: all temp
	echo "Main-Class: VirtualHomeBrew3" >Manifest.txt
	jar -cmf Manifest.txt VirtualHomeBrew3.jar *.class \
		*.rom \
		-C temp .
	rm -rf temp

temp: __FRC__
	mkdir -p temp
	rm -rf temp/*
	cp -r docs temp/
	rm -f temp/docs/z80mc.jpg
	./mkAbout >temp/docs/About.html
	cd temp; jar -xf ../z80cpu/z80core.jar
	rm -rf temp/META-INF

z80cpu/z80core.jar:
	make -C z80cpu z80core.jar

jar: VirtualHomeBrew3.jar

www: __FRC__
	false

ship: __FRC__
	rsync -Wuv --copy-links VirtualHomeBrew3.jar homebrew3.rom ../bin

__FRC__: