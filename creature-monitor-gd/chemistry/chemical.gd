extends Control

export var chemical_id: int = 0

func setup(cid: int):
	chemical_id = cid
	$VBC/CenterContainer/CatalogueLabel.offset = chemical_id
	ChemicalTracker.connect("snapshot_updated", self, "_snapshot_updated")
	_snapshot_updated()

func _snapshot_updated():
	var v = ChemicalTracker.snapshot.chemicals[chemical_id]
	$VBC/CenterContainer/pb.value = v
	hint_tooltip = str(chemical_id) + ": " + str(v)
