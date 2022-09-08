extends SpinBox

func _ready():
	ChemicalTracker.connect("time_range_updated", self, "_tru")
	_tru()
	connect("value_changed", self, "_vc")

func _tru():
	value = ChemicalTracker.history_time_range

func _vc(r: float):
	ChemicalTracker.history_time_range = r
