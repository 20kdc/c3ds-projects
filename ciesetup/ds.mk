
ds: $(R)pkg_dockingstation.tar $(R)pkg_engine.tar

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

# -- Gadget set 3: Engine package --

INTERMEDIATES += $(R)pkg_engine.tar

$(R)pkg_engine.tar: $(R)ds_195_64_dec.tar gadgets/prep_engine.py gadgets/dummy.so gadgets/runtime.so gadgets/runtime_headless.so gadgets/run-game
	# alright, so, this is where things get hairy, because BASICALLY,
	#  their script assumes you want separate global/user directories
	# we don't want this because it makes agent install act funny IIRC?
	# but if we nuke their script we have to take over for it, and that's where the workarounds come in
	mkdir -p $(R)tmp_engine
	cd $(R)tmp_engine ; tar -xf ../ds_195_64_dec.tar
	# time to do modifications HERE
	cd $(R)tmp_engine ; python3 ../$(UNR)gadgets/prep_engine.py ../$(UNR)gadgets/
	# done with modifications, save
	cd $(R)tmp_engine ; tar -cf ../pkg_engine.tar_ .
	rm -rf $(R)tmp_engine
	mv $(R)pkg_engine.tar_ $(R)pkg_engine.tar

# -- Gadget set 4: DS package --

INTERMEDIATES += $(R)pkg_dockingstation.tar

$(R)pkg_dockingstation.tar: $(R)ds_195_64_dec.tar gadgets/prep_dockingstation.py gadgets/preplib.py ../efw-integration/efw.zip
	# similar to the engine package, have to mess around a lot to make it work
	mkdir -p $(R)tmp_dockingstation
	cd $(R)tmp_dockingstation ; tar -xf ../ds_195_64_dec.tar
	# time to do modifications HERE
	cd $(R)tmp_dockingstation ; python3 ../$(UNR)gadgets/prep_dockingstation.py ../$(UNR)gadgets/
	# update to new server
	cd $(R)tmp_dockingstation ; unzip ../$(UNR)../efw-integration/efw.zip
	# rearrange
	mkdir -p $(R)tmp_pkg_dockingstation
	mv $(R)tmp_dockingstation "$(R)tmp_pkg_dockingstation/Docking Station"
	# done with modifications, save
	cd $(R)tmp_pkg_dockingstation ; tar -cf ../pkg_dockingstation.tar_ .
	rm -rf $(R)tmp_pkg_dockingstation
	mv $(R)pkg_dockingstation.tar_ $(R)pkg_dockingstation.tar

