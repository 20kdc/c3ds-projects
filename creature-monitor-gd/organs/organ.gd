extends Control

export var organ_id = -1

onready var kill_btn = $kill
onready var heal_btn = $heal

func _ready():
	kill_btn.prefix_targ_creature = true
	kill_btn.caos = "injr " + str(organ_id) + " 5000000000"
	heal_btn.prefix_targ_creature = true
	heal_btn.caos = "injr " + str(organ_id) + " -5000000000"

func _on_Label2_toggled(button_pressed):
	kill_btn.disabled = not button_pressed

func update_status(lf: float):
	$Label.text = "Organ " + str(organ_id) + " " + str(lf)
