extends Node

var time = 0.0
var req: CPXRequest
var last_request: CPXRequest

var snapshot: ChemicalSnapshot

signal snapshot_updated()

func _init():
	snapshot = ChemicalSnapshot.new()

func _process(delta):
	# update ongoing requests
	if req != null:
		if req.poll():
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
		if mon != "":
			req = CPXRequest.new(ChemicalSnapshot.snapshot_request(mon))
			last_request = req
