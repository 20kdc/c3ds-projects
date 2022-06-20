# ciesetup

R = repo/
UNR = ../

INTERMEDIATES :=
INPUTS := $(R)dockingstation.run.tar.bz2 $(R)dockingstation.run $(R)dockingstation_195_64.tar.bz2

# Global rules

default: $(R)unified.tar

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

# "Installs" the game. Kind of. See the script for details.
$(R)ds_195_64_dec.tar: gadgets/debz2.py $(R)ds_195_64.tar
	python3 gadgets/debz2.py $(R)ds_195_64.tar $(R)ds_195_64_dec.tar_
	mv $(R)ds_195_64_dec.tar_ $(R)ds_195_64_dec.tar

# -- Gadget set 3: Librarian --

INTERMEDIATES += $(R)libraries.tar

# notes for future work: it's possible to symlink these to GTK2, but it fails when it actually tries to *use* GTK2.
# is it possible to make some sort of shim? maybe "libdummy.so" + preloading?
# FOR NOW, we expect the bare minimum necessary files.
# IN FUTURE these should be acquired or built somehow.
$(R)libraries.tar: $(R)libgdk-1.2.so.0 $(R)libglib-1.2.so.0 $(R)libgmodule-1.2.so.0 $(R)libgthread-1.2.so.0 $(R)libgtk-1.2.so.0
	cd $(R) ; tar -cf libraries.tar_ ./libgdk-1.2.so.0 ./libglib-1.2.so.0 ./libgmodule-1.2.so.0 ./libgthread-1.2.so.0 ./libgtk-1.2.so.0
	mv $(R)libraries.tar_ $(R)libraries.tar

# -- Gadget set 4: Workarounds --

INTERMEDIATES += $(R)ds_195_64_wa.tar

$(R)ds_195_64_wa.tar: $(R)ds_195_64_dec.tar gadgets/workarounds.py
	# alright, so, this is where things get hairy, because BASICALLY,
	#  their script assumes you want separate global/user directories
	# we don't want this because it makes agent install act funny IIRC?
	# but if we nuke their script we have to take over for it, and that's where the workarounds come in
	mkdir -p $(R)tmp_ds_195_64_wa
	cd $(R)tmp_ds_195_64_wa ; tar -xf ../ds_195_64_dec.tar
	# time to do modifications HERE
	cd $(R)tmp_ds_195_64_wa ; python3 ../$(UNR)gadgets/workarounds.py
	# done with modifications, save
	cd $(R)tmp_ds_195_64_wa ; tar -cf ../ds_195_64_wa.tar_ .
	rm -rf $(R)tmp_ds_195_64_wa
	mv $(R)ds_195_64_wa.tar_ $(R)ds_195_64_wa.tar

# -- Unification --

INTERMEDIATES += $(R)unified.tar

$(R)unified.tar: $(R)libraries.tar $(R)ds_195_64_wa.tar
	cd $(R) ; tar -Af unified.tar_ ds_195_64_wa.tar
	cd $(R) ; tar -Af unified.tar_ libraries.tar
	mv $(R)unified.tar_ $(R)unified.tar

