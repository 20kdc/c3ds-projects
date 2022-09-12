class_name BrainSnapshot
extends Reference

# metadata
var bytes: int
var config: BrainConfig

# of BrainLobeSnapshot
var lobes: Array
# of BrainTractSnapshot
var tracts: Array

static func snapshot_caos(cfg: BrainConfig, moniker: String) -> String:
	var caos = ""
	caos += "targ mtoc \"" + moniker + "\"\n"
	caos += "setv va00 0 reps " + str(cfg.lobe_count) + " brn: dmpl va00 addv va00 1 repe\n"
	caos += "setv va00 0 reps " + str(cfg.tract_count) + " brn: dmpt va00 addv va00 1 repe\n"
	return caos

func import(c: BrainConfig, req: CPXRequest):
	config = c
	bytes = len(req.result)
	# the actual contents, please
	var stream = StreamPeerBuffer.new()
	stream.big_endian = false
	stream.data_array = req.result

	lobes = []
	for _idx in c.lobe_range:
		var lobe = BrainLobeSnapshot.new()
		lobe.import(stream)
		lobes.push_back(lobe)

	tracts = []
	for _idx in c.tract_range:
		var tract = BrainTractSnapshot.new()
		tract.import(stream)
		tracts.push_back(tract)
