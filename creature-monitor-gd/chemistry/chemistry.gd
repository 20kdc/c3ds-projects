extends Control

onready var gw: GraphWidget = $gv/Control
var tracking_chemical: int = 0

func _ready():
	ChemicalTracker.connect("chemistry_graph_should_show", self, "_cgss")
	for v in range(255):
		var chem = preload("chemical.tscn").instance()
		chem.chemical_id = v + 1
		$tc/All/gc.add_child(chem)

func _process(_delta):
	if ChemicalTracker.last_request != null:
		$gv/CPXErrorBox.update_from(ChemicalTracker.last_request)

func _cgss(chemical_id: int):
	tracking_chemical = chemical_id
	$gv/HBoxContainer/CatalogueLabel.offset = chemical_id
	gw.drawing_area.graph_line = ChemicalTracker.chemical_history(chemical_id)
