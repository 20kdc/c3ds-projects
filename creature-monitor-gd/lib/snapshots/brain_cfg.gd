class_name BrainConfig
extends Reference

# BIG SCARY NOTE:
# Theoretically, the mythical DUMP V1.0 flag would go here.
# However, the chance of this mattering is effectively nil...

var lobe_count: int = 0
var lobe_range: Array
var tract_count: int = 0
var tract_range: Array

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
	lobe_count = int(line1a.get_string_from_ascii())
	lobe_range = range(lobe_count)
	tract_count = int(line2a.get_string_from_ascii())
	tract_range = range(tract_count)

func splitoff(a: PoolByteArray) -> Array:
	var firstzero = a.find(0)
	if firstzero == -1:
		return [a, PoolByteArray()]
	var b: PoolByteArray = a
	b.resize(firstzero)
	return [b, a.subarray(firstzero + 1, -1)]
