extends Control

func _ready():
	for v in range(255):
		var chem = preload("chemical.tscn").instance()
		chem.get_node("VBC").get_node("CatalogueLabel").offset = v + 1
		$sc/gc.add_child(chem)
