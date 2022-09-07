extends Control

onready var gw: GraphWidget = $gv/Control
var tracking_chemical: int = 0

func _ready():
	gw.clear_points()
	ChemicalTracker.connect("chemistry_graph_should_show", self, "_cgss")
	ChemicalTracker.connect("snapshot_updated", self, "_snapshot")
	for v in range(255):
		var chem = preload("chemical.tscn").instance()
		chem.chemical_id = v + 1
		$tc/All/gc.add_child(chem)

func _process(_delta):
	if ChemicalTracker.last_request != null:
		$gv/CPXErrorBox.update_from(ChemicalTracker.last_request)

func _cgss(chemical_id: int):
	gw.clear_points()
	tracking_chemical = chemical_id

func _snapshot():
	if tracking_chemical != 0:
		gw.add_point(ChemicalTracker.snapshot.time, ChemicalTracker.snapshot.chemicals[tracking_chemical])
