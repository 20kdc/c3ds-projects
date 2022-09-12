extends Label

func update_from_snapshot(snapshot: BrainSnapshot):
	text = "LC " + str(snapshot.config.lobe_count) + "\nTC " + str(snapshot.config.tract_count) + "\nSnapshot bytes: " + str(snapshot.bytes) + "\n"
	for v in snapshot.lobes:
		var lobe: BrainLobeSnapshot = v
		text += lobe.name + " LOBE W:" + str(lobe.w) + "x" + str(lobe.h) + "\n"
	for v in snapshot.tracts:
		var tract: BrainTractSnapshot = v
		text += "TRACT " + snapshot.lobes[tract.src_lobe].name + " -> " + snapshot.lobes[tract.dst_lobe].name + "\n"
