extends Control

var cc_moniker = ""
var cc_name = ""

onready var f_moniker: Button = find_node("f_moniker")
onready var f_name: Label = find_node("f_name")
onready var f_types: Label = find_node("f_types")
onready var f_age: Label = find_node("f_age")
onready var f_status: Label = find_node("f_status")

func _ready():
	f_moniker.connect("pressed", self, "_pressed")

func _pressed():
	TargetCreature.set_creature(cc_moniker, cc_name)

func update_norn(k: String, norn: String, infos: Array):
	if k == norn:
		add_stylebox_override("panel", preload("selected_norn.tres"))
	else:
		add_stylebox_override("panel", preload("not_selected_norn.tres"))
	f_moniker.text = k
	f_name.text = infos[1]
	f_types.tag = "Agent Help 4 " + infos[2] + " " + infos[3]
	f_age.offset = int(infos[4])
	if infos[5] == "0":
		f_status.text = "OK"
	else:
		f_status.text = "DEAD"
	cc_moniker = k
	cc_name = infos[1]
