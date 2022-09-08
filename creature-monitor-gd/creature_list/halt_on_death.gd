extends CheckBox

func _ready():
	TargetCreature.connect("creature_dead_notify", self, "_cdn")

func _cdn(_mon):
	if pressed:
		$"../dbg_paws"._pressed()
		pressed = false
