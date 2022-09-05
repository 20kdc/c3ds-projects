extends Label

func _ready():
	TargetCreature.connect("target_creature_changed", self, "_tcc")
	_tcc()

func _tcc():
	if TargetCreature.moniker != "":
		text = TargetCreature.given_name + " (" + TargetCreature.moniker + ")"
	else:
		text = ""
