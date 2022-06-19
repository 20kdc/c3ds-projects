# ciesetup

R = repo/

INTERMEDIATES :=
INPUTS := $(R)dockingstation.run.tar.bz2 $(R)dockingstation.run $(R)dockingstation_195_64.tar.bz2

# Global rules

default: $(R)ds_195_64_dec.tar

.PHONY: clean

clean:
	rm -f $(INTERMEDIATES)

# -- Gadget set 1: Conversion of inputs to common ds_195_64.tar form --

INTERMEDIATES += $(R)ds_195_64.tar
INTERMEDIATES += $(R)dockingstation_195_64.tar

# Get rid of the Makeself stuff
$(R)dockingstation.run.tar.bz2: $(R)dockingstation.run
	tail -c +8778 $(R)dockingstation.run > $(R)dockingstation.run.tar.bz2_
	mv $(R)dockingstation.run.tar.bz2_ $(R)dockingstation.run.tar.bz2

# This is the canonical form. 22794240 bytes.
ds_195_64.tar.1: $(R)dockingstation.run.tar.bz2
	bunzip2 < $(R)dockingstation.run.tar.bz2 > $(R)ds_195_64.tar_
	mv $(R)ds_195_64.tar_ $(R)ds_195_64.tar

# This is in a slightly different form, we need to convert it across.
# Also 22794240 bytes.
ds_195_64.tar.2: $(R)dockingstation_195_64.tar.bz2
	mkdir -p $(R)tmp_ds_195_64
	cd $(R)tmp_ds_195_64 ; tar -jxf ../dockingstation_195_64.tar.bz2
	cd $(R)tmp_ds_195_64/dockingstation_195_64 ; tar -cf ../../ds_195_64.tar_ .
	rm -rf $(R)tmp_ds_195_64
	mv $(R)ds_195_64.tar_ $(R)ds_195_64.tar

# Now we need to unify the two.
$(R)ds_195_64.tar:
	make ds_195_64.tar.1 || make ds_195_64.tar.2

# -- Gadget set 2: I Can't Believe It's Not InstallBurst --

# Un-bzip2s everything, LOWERCASES ALL THE FILENAMES (!!!), unifies port and main trees
$(R)ds_195_64_dec.tar: gadgets/debz2.py $(R)ds_195_64.tar
	python3 gadgets/debz2.py $(R)ds_195_64.tar $(R)ds_195_64_dec.tar_
	mv $(R)ds_195_64_dec.tar_ $(R)ds_195_64_dec.tar

# -- Gadget set 3: Librarian --

# wuh-oh
# notes for future work: libraries should be a separate TAR file set that then gets merged with the main tree at the end
# additional notes: it's possible to symlink these to GTK2, but it fails when it actually tries to *use* GTK2.
# is it possible to make some sort of shim? maybe "libdummy.so" + preloading?

# -- Gadget set 4: Workarounds --

# notes for future work: need dstation-install to either be more chill or to conveniently get deleted and replaced with something entirely different
# need to do the DS_music workaround if dstation-install isn't doing it
# need to make langpick happen or take over for it
# need to take over the duties of managing C3 installation

