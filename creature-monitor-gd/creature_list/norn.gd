extends Control

var cc_moniker = ""
var cc_name = ""

func _ready():
	$ipc/hb/moniker.connect("pressed", self, "_pressed")

func _pressed():
	CM2Globals.set_current_creature(cc_moniker, cc_name)

func update_norn(k: String, norn: String, infos: Array):
	if k == norn:
		add_stylebox_override("panel", preload("selected_norn.tres"))
	else:
		add_stylebox_override("panel", preload("not_selected_norn.tres"))
	var ag = "Agent Help 4 " + infos[2] + " " + infos[3]
	$ipc/hb/moniker.text = k
	$ipc/hb/detail.text = infos[1] + "\n" + CatalogueTracker.lookup(ag, 0) + "\n" + CatalogueTracker.lookup("creature_history_life_stage", int(infos[4]))
	cc_moniker = k
	cc_name = infos[1]
