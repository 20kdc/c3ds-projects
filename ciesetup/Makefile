# ciesetup

R = repo/
UNR = ../

INTERMEDIATES :=

# Global rules

default: ds

test: $(R)unified.tar
	mkdir -p /media/ramdisk/ds
	tar -xf $(R)unified.tar -C /media/ramdisk/ds
	cd /media/ramdisk/ds ; ./dockingstation

.PHONY: clean

clean:
	rm -f $(INTERMEDIATES)

include ds.mk
include c3.mk

