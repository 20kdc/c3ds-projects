extends Label

func _ready():
	CM2Globals.connect("target_creature_changed", self, "_tcc")
	_tcc()

func _tcc():
	if CM2Globals.target_creature_moniker != "":
		text = CM2Globals.target_creature_name + " (" + CM2Globals.target_creature_moniker + ")"
	else:
		text = ""
