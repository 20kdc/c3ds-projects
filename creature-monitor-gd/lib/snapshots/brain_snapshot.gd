class_name BrainSnapshot
extends Reference

# metadata
var bytes: int
var config: BrainConfig

# of BrainLobeSnapshot
var lobes: Array
# of BrainTractSnapshot
var tracts: Array

func _init():
	lobes = []
	tracts = []

static func snapshot_caos(config: BrainConfig, moniker: String) -> String:
	var caos = ""
	caos += "targ mtoc \"" + moniker + "\"\n"
	caos += "setv va00 0 reps " + str(config.lobe_count) + " brn: dmpl va00 addv va00 1 repe\n"
	caos += "setv va00 0 reps " + str(config.tract_count) + " brn: dmpt va00 addv va00 1 repe\n"
	return caos

func import(c: BrainConfig, req: CPXRequest):
	config = c
	bytes = len(req.result)
	# the actual contents, please
	var stream = StreamPeerBuffer.new()
	stream.big_endian = false
	stream.data_array = req.result
	# TODO: Read in data from lobes & tracts
