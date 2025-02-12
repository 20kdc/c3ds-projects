# Yes, pretty much everything here is cross-compiled to Windows from Linux.
# This is because pretty much all of it is mucking around with old stuff that's
# usually Windows-only and at best has an ancient Linux port.

include tools.mk

INTERMEDIATES := release-id.txt

all:

# Release All
rel-all: rel rel-sdk rel-fscc

# Release (Full)
rel: COPYING.txt README.md CREDITS.txt assorted-caos
	echo "r`date +%s` (Full)" > release-id.txt
	echo "Linux host:" $(HOST_LINUX) >> release-id.txt
	echo "Godot available:" $(HOST_GODOT) >> release-id.txt
	echo "mdbook available:" $(HOST_MDBOOK) >> release-id.txt
	echo "Java/Maven available:" $(HOST_JAVA) >> release-id.txt
	echo "git status:" >> release-id.txt
	git status >> release-id.txt
	echo "Latest commit:" >> release-id.txt
	git log HEAD^..HEAD >> release-id.txt
	rm -f release.zip
	zip -r release.zip $^
	zip release.zip release-id.txt

# Release (SDK)
rel-sdk: COPYING.txt README.md CREDITS.txt assorted-caos
	echo "r`date +%s` (SDK)" > release-id.txt
	echo "Linux host:" $(HOST_LINUX) >> release-id.txt
	echo "mdbook available:" $(HOST_MDBOOK) >> release-id.txt
	echo "Java/Maven available:" $(HOST_JAVA) >> release-id.txt
	echo "git status:" >> release-id.txt
	git status >> release-id.txt
	echo "Latest commit:" >> release-id.txt
	git log HEAD^..HEAD >> release-id.txt
	rm -f sdk.zip
	zip -r sdk.zip $^
	zip sdk.zip release-id.txt

# Release (FSCC)
rel-fscc: COPYING.txt README.md CREDITS.txt
	echo "r`date +%s` (festival edition)" > release-id.txt
	echo "Linux host:" $(HOST_LINUX) >> release-id.txt
	echo "Java/Maven available:" $(HOST_JAVA) >> release-id.txt
	echo "git status:" >> release-id.txt
	git status >> release-id.txt
	echo "Latest commit:" >> release-id.txt
	git log HEAD^..HEAD >> release-id.txt
	rm -f fscc.zip
	zip -r fscc.zip $^
	zip fscc.zip release-id.txt

include colour-depth-fix/index.mk
include caosproxy/index.mk
include python/index.mk
include efw-integration/index.mk

ifeq ($(HOST_GODOT),1)
include creature-monitor-gd-export/index.mk
endif

ifeq ($(HOST_MDBOOK),1)
include tob/index.mk
endif

ifeq ($(HOST_JAVA),1)
.PHONY: java
java:
	./umvn package
include ral/index.mk
include cdsp-tools/index.mk
endif

# Stuff that has compilation steps that require Linux.
# As such these will basically just assume you have Linux for everything.
# Note that I'm saying "Linux" here, not BSDs - this stuff won't work for 'em anyway.
# Sorry! ^.^;
ifeq ($(HOST_LINUX),1)
include ciesetup/index.mk
endif

rel-fscc: $(CAOSPROX_REL)
rel-fscc: $(JTOOLS_REL)

clean:
	rm -f $(INTERMEDIATES)

