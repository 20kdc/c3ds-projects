class_name BrainConfig
extends Resource

# BIG SCARY NOTE:
# Theoretically, the mythical DUMP V1.0 flag would go here.
# However, the chance of this mattering is effectively nil...

export var lobe_count: int = 0 setget set_lobe_count
# of int
var lobe_range: Array
export var tract_count: int = 0 setget set_tract_count
# of int
var tract_range: Array

func set_lobe_count(lc: int):
	lobe_count = lc
	lobe_range = range(lc)

func set_tract_count(lc: int):
	tract_count = lc
	tract_range = range(lc)

static func snapshot_caos(moniker: String) -> String:
	var caos = ""
	caos += "targ mtoc \"" + moniker + "\"\n"
	caos += """
		brn: dmpb
	"""
	return caos

func import(req: CPXRequest):
	var line1 = splitoff(req.result)
	var line1a: PoolByteArray = line1[0]
	var line2 = splitoff(line1[1])
	var line2a: PoolByteArray = line2[0]
	set_lobe_count(int(line1a.get_string_from_ascii()))
	set_tract_count(int(line2a.get_string_from_ascii()))

func splitoff(a: PoolByteArray) -> Array:
	var firstzero = a.find(0)
	if firstzero == -1:
		return [a, PoolByteArray()]
	var b: PoolByteArray = a
	b.resize(firstzero)
	return [b, a.subarray(firstzero + 1, -1)]
