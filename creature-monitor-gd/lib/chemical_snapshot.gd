class_name ChemicalSnapshot
extends Reference

# Chemicals (by ID)
var chemicals: PoolRealArray

func _init():
	chemicals.resize(256)
	chemicals.fill(0)

static func snapshot_request(moniker: String) -> PoolByteArray:
	var caos = ""
	caos += "targ mtoc \"" + moniker + "\"\n"
	caos += """
		setv va00 0
		loop
		outv chem va00
		outs "\\n"
		addv va00 1
		untl va00 eq 256
	"""
	return CPXRequest.from_caos(caos)

func import(req: CPXRequest):
	var idx = 0
	for v in req.result_str().split("\n"):
		if idx >= 256:
			break
		chemicals[idx] = float(v)
		idx += 1
