# ciesetup

R = repo/
UNR = ../

INTERMEDIATES := $(R)unified.tar

# Global rules

default: ds

$(R)unified.tar: $(R)pkg_creatures3.tar $(R)pkg_dockingstation.tar $(R)pkg_engine.tar
	rm -f $(R)unified.tar
	tar --concatenate -f $(R)unified.tar $(R)pkg_engine.tar
	tar --concatenate -f $(R)unified.tar $(R)pkg_dockingstation.tar
	tar --concatenate -f $(R)unified.tar $(R)pkg_creatures3.tar

test: $(R)unified.tar
	mkdir -p /media/ramdisk/ds
	tar -xf $(R)unified.tar -C /media/ramdisk/ds
	cd /media/ramdisk/ds ; ./dockingstation

.PHONY: clean

clean:
	rm -f $(INTERMEDIATES)

include ds.mk
include c3.mk

