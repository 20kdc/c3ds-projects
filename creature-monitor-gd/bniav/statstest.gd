extends Label

func update_from_snapshot(snapshot: BrainSnapshot):
	text = str(snapshot.config.lobe_count) + " lobes, " + str(snapshot.config.tract_count) + " tracts\n"
	text += "Raw data bytes: " + str(snapshot.bytes) + "\n"
	text += "\n"
	text += "Lobes:\n"
	var idx = 0
	var total_neurons = 0
	for v in snapshot.lobes:
		var lobe: BrainLobeSnapshot = v
		text += " " + str(idx) + ": " + lobe.name + " " + str(lobe.w) + "x" + str(lobe.h) + "\n"
		idx += 1
		total_neurons += lobe.w * lobe.h
	text += "\n"
	text += str(total_neurons) + " total neurons\n"
	text += "\n"
	text += "Tracts:\n"
	var total_dendrites = 0
	idx = 0
	for v in snapshot.tracts:
		var tract: BrainTractSnapshot = v
		var dc = len(tract.dendrites)
		text += " " + str(idx) + ": " + snapshot.lobes[tract.src_lobe].name + " -> " + snapshot.lobes[tract.dst_lobe].name + ", " + str(dc) + " dendrites\n"
		total_dendrites += dc
		idx += 1
	text += "\n"
	text += str(total_dendrites) + " total dendrites"
