extends Control

var cc_moniker = ""
var cc_name = ""

func _ready():
	$ipc/hb/moniker.connect("pressed", self, "_pressed")

func _pressed():
	TargetCreature.set_creature(cc_moniker, cc_name)

func update_norn(k: String, norn: String, infos: Array):
	if k == norn:
		add_stylebox_override("panel", preload("selected_norn.tres"))
	else:
		add_stylebox_override("panel", preload("not_selected_norn.tres"))
	$ipc/hb/moniker.text = k
	$ipc/hb/dt/name.text = infos[1]
	$ipc/hb/dt/types.tag = "Agent Help 4 " + infos[2] + " " + infos[3]
	$ipc/hb/dt/age.offset = int(infos[4])
	cc_moniker = k
	cc_name = infos[1]
