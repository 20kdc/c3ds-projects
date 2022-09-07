extends Node

enum DISPOSITION {
	IGNORE,
	ZERO,
	ONE
}

var time = 0.0
var req: CPXRequest
var last_request: CPXRequest

var snapshot: ChemicalSnapshot
var dispositions: PoolIntArray
var disposition_moniker: String = ""

signal snapshot_updated()
signal dispositions_updated()

func _init():
	snapshot = ChemicalSnapshot.new()
	dispositions.resize(256)
	dispositions.fill(DISPOSITION.IGNORE)

func set_disposition(chemical_id: int, val: int):
	dispositions[chemical_id] = val
	disposition_moniker = TargetCreature.moniker
	emit_signal("dispositions_updated")

func _process(delta):
	# update ongoing requests
	if req != null:
		if req.poll():
			# Note that this is updated here
			# The error dialog kept flashing in the interim
			last_request = req
			if req.result_code == 0:
				snapshot.import(req)
				emit_signal("snapshot_updated")
			req = null
		else:
			return
	# spawn next request
	time += delta
	if time > 0.05:
		time = 0
		var mon = TargetCreature.moniker
		if mon != disposition_moniker:
			# clear chemical dispositions if user changed target creature
			# to prevent "accidents"
			dispositions.fill(DISPOSITION.IGNORE)
			disposition_moniker = mon
			emit_signal("dispositions_updated")
		if mon != "":
			var code = ChemicalSnapshot.snapshot_caos(mon)
			var chem_idx = 0
			for chem_disposition in dispositions:
				if chem_disposition == DISPOSITION.ONE:
					code += "\nchem " + str(chem_idx) + " 1"
				elif chem_disposition == DISPOSITION.ZERO:
					code += "\nchem " + str(chem_idx) + " -1"
				chem_idx += 1
			req = CPXRequest.new(CPXRequest.from_caos(code))
