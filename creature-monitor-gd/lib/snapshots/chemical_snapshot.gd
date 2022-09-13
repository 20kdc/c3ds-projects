class_name ChemicalSnapshot
extends Resource

export var time: float = 0

# Chemicals (by ID)
export var chemicals: PoolRealArray

func _init():
	chemicals.resize(256)
	chemicals.fill(0)

static func snapshot_caos(moniker: String) -> String:
	var caos = ""
	caos += "targ mtoc \"" + moniker + "\"\n"
	caos += """
		outv wtik
		outs "\\n"
		setv va00 0
		loop
		outv chem va00
		outs "\\n"
		addv va00 1
		untl va00 eq 256
	"""
	return caos

func import(req: CPXRequest):
	var idx = -1
	for v in req.result_str().split("\n"):
		var vf = float(v)
		if idx == -1:
			time = vf
		elif idx >= 256:
			break
		else:
			chemicals[idx] = vf
		idx += 1
