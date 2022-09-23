
c3: $(R)c3u2lin.tar

# -- Gadget set 1: C3U2W32 --

# Prepare a Creatures 3 Update 2 W32 archive so we have something consistent to convert

INTERMEDIATES += $(R)c3u2w32.tar

$(R)c3u2w32.tar: $(R)Creatures3_Update2.exe $(R)CREATURES_3.iso
	mkdir -p $(R)tmp_c3u2w32
	# start with unpacking C3 data as-is
	cd $(R)tmp_c3u2w32 ; 7z x -r ../CREATURES_3.iso Install/Install
	cd $(R)tmp_c3u2w32 ; 7z x -r ../Creatures3_Update2.exe
	# Now do rearrangement - C3
	cd $(R)tmp_c3u2w32 ; mv Install/Install/* ./ ; rmdir Install/Install ; rmdir Install
	# C3u2 deletions
	rm "$(R)tmp_c3u2w32/Bootstrap/001 World/creator.cos"
	rm "$(R)tmp_c3u2w32/Genetics/ettn.final45e.gen.brain.gen" "$(R)tmp_c3u2w32/Genetics/ettn.final45e.gen.brain.gno"
	rm "$(R)tmp_c3u2w32/Genetics/gren.final45g.gen.brain.gen" "$(R)tmp_c3u2w32/Genetics/gren.final45g.gen.brain.gno"
	rm "$(R)tmp_c3u2w32/Genetics/norn.bengal45.gen.brain.gen" "$(R)tmp_c3u2w32/Genetics/norn.bengal45.gen.brain.gno"
	rm "$(R)tmp_c3u2w32/Genetics/norn.bruin45.gen.brain.gen" "$(R)tmp_c3u2w32/Genetics/norn.bruin45.gen.brain.gno"
	rm "$(R)tmp_c3u2w32/Genetics/norn.civet45.gen.brain.gen" "$(R)tmp_c3u2w32/Genetics/norn.civet45.gen.brain.gno"
	# Not supposed to be here
	rm "$(R)tmp_c3u2w32/Patcher.exe"
	# C3u2
	cd $(R)tmp_c3u2w32 ; mv "Resource Files Directory"/* "My Agents/" ; rmdir "Resource Files Directory"
	cd $(R)tmp_c3u2w32 ; mv "Main Directory"/* ./ ; rmdir "Main Directory"
	cd $(R)tmp_c3u2w32 ; mv "Images Directory"/* Images/ ; rmdir "Images Directory"
	cd $(R)tmp_c3u2w32 ; mv "Genetics Directory"/* Genetics/ ; rmdir "Genetics Directory"
	cd $(R)tmp_c3u2w32 ; mv "Catalogue Directory"/* Catalogue/ ; rmdir "Catalogue Directory"
	cd $(R)tmp_c3u2w32 ; cp -r "Bootstrap Directory"/* ./Bootstrap/ ; rm -rf "Bootstrap Directory"
	# done, save
	cd $(R)tmp_c3u2w32 ; tar -cf ../c3u2w32.tar_ .
	rm -rf $(R)tmp_c3u2w32
	mv $(R)c3u2w32.tar_ $(R)c3u2w32.tar

# -- Gadget set 2: C3U2LIN --

INTERMEDIATES += $(R)c3u2lin.tar

$(R)c3u2lin.tar: gadgets/w32lin.py $(R)c3u2w32.tar
	python3 gadgets/w32lin.py $(R)c3u2w32.tar $(R)c3u2lin.tar_
	mv $(R)c3u2lin.tar_ $(R)c3u2lin.tar

