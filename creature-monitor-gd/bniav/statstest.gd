extends Label

func update_from_snapshot(snapshot: BrainSnapshot):
	text = "LC " + str(snapshot.config.lobe_count) + "\nTC " + str(snapshot.config.tract_count) + "\nSnapshot bytes: " + str(snapshot.bytes)
