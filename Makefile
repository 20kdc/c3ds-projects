# Yes, pretty much everything here is cross-compiled to Windows from Linux.
# This is because pretty much all of it is mucking around with old stuff that's
# usually Windows-only and at best has an ancient Linux port.

COMPILER ?= i686-w64-mingw32-gcc
STRIP ?= i686-w64-mingw32-strip
WINDRES ?= i686-w64-mingw32-windres
NASM ?= nasm

COMPILE_FLAGS_DLL ?= -Os -flto -static-libgcc -Wno-multichar -shared
COMPILE_FLAGS_EXE ?= -Os -flto -static-libgcc -Wno-multichar
COMPILE_FLAGS_GUI ?= -Os -flto -static-libgcc -Wno-multichar -mwindows

INTERMEDIATES :=

all:

rel: COPYING.txt README.md
	echo `date +%s` > release-id.txt
	rm -f release.zip
	zip release.zip $^
	zip release.zip release-id.txt

include colour-depth-fix/index.mk
include caosproxy/index.mk

clean:
	rm -f $(INTERMEDIATES)

