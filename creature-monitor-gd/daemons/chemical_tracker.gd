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
var history: Array
var _last_moniker: String = ""
var chemical_range: Array
var history_time_range: float = 100 setget set_history_time_range

signal snapshot_updated()
signal dispositions_updated()
signal time_range_updated()

# just for routing, emitted from chemical.gd
# warning-ignore:unused_signal
signal chemistry_graph_should_show(chemical_id)

func _init():
	snapshot = ChemicalSnapshot.new()
	dispositions.resize(256)
	dispositions.fill(DISPOSITION.IGNORE)
	history = []
	chemical_range = range(256)
	for _i in chemical_range:
		var cgl = CMGraphLine.new()
		cgl.time_range = history_time_range
		history.push_back(cgl)

func set_history_time_range(f: float):
	history_time_range = f
	for v in history:
		v.time_range = history_time_range
	emit_signal("time_range_updated")

func _ready():
	TargetCreature.connect("target_creature_changed", self, "_target_creature_changed")

func chemical_history(id: int) -> CMGraphLine:
	return history[id]

func _target_creature_changed():
	# debounce name changes
	if TargetCreature.moniker != _last_moniker:
		# clear chemical dispositions if user changed target creature
		# to prevent "accidents"
		dispositions.fill(DISPOSITION.IGNORE)
		_last_moniker = TargetCreature.moniker
		emit_signal("dispositions_updated")
		for v in history:
			var gl: CMGraphLine = v
			gl.clear()

func set_disposition(chemical_id: int, val: int):
	dispositions[chemical_id] = val
	_last_moniker = TargetCreature.moniker
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
				for i in chemical_range:
					var gl: CMGraphLine = history[i]
					if gl.points() != 0 and gl.latest().x > snapshot.time:
						# we went back in time, something's wrong!
						gl.clear()
					gl.add(snapshot.time, snapshot.chemicals[i])
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
			var code = ChemicalSnapshot.snapshot_caos(mon)
			var chem_idx = 0
			for chem_disposition in dispositions:
				if chem_disposition == DISPOSITION.ONE:
					code += "\nchem " + str(chem_idx) + " 1"
				elif chem_disposition == DISPOSITION.ZERO:
					code += "\nchem " + str(chem_idx) + " -1"
				chem_idx += 1
			req = CPXRequest.new(CPXRequest.from_caos(code))
