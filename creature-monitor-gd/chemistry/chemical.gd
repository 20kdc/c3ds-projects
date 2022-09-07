extends Control

export var chemical_id: int = 0

func _ready():
	$VBC/CenterContainer/CatalogueLabel.offset = chemical_id
	ChemicalTracker.connect("snapshot_updated", self, "_snapshot_updated")
	ChemicalTracker.connect("dispositions_updated", self, "_dispositions_updated")
	_snapshot_updated()
	_dispositions_updated()

func _snapshot_updated():
	var v = ChemicalTracker.snapshot.chemicals[chemical_id]
	$VBC/CenterContainer/pb.value = v
	hint_tooltip = str(chemical_id) + ": " + str(v)

func _dispositions_updated():
	$VBC/override.selected = ChemicalTracker.dispositions[chemical_id]

func _on_override_item_selected(index):
	ChemicalTracker.set_disposition(chemical_id, index)

func _gui_input(event):
	if event is InputEventMouseButton:
		if event.button_index == BUTTON_LEFT and event.pressed:
			ChemicalTracker.emit_signal("chemistry_graph_should_show", chemical_id)
