# Yes, pretty much everything here is cross-compiled to Windows from Linux.
# This is because pretty much all of it is mucking around with old stuff that's
# usually Windows-only and at best has an ancient Linux port.

include tools.mk

INTERMEDIATES := release-id.txt

all:

rel: COPYING.txt README.md
	echo `date +%s` > release-id.txt
	rm -f release.zip
	zip release.zip $^
	zip release.zip release-id.txt

rel: thirdparty/COPYING-SDL.txt thirdparty/README-SDL.txt

include colour-depth-fix/index.mk
include caosproxy/index.mk
include creature-monitor/index.mk

# Stuff that has compilation steps that require Linux.
# As such these will basically just assume you have Linux for everything.
# Note that I'm saying "Linux" here, not BSDs - this stuff won't work for 'em anyway.
# Sorry! ^.^;
ifeq ($(HOST_LINUX),1)
	include ciesetup/index.mk
endif

clean:
	rm -f $(INTERMEDIATES)

