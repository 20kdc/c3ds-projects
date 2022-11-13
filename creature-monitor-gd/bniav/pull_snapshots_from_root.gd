extends Node

onready var px: BNIAVMainBrainView = get_parent()

func _on_Brain_snapshot_updated(snapshot):
	px.snapshot = snapshot

func _on_CheckBox_toggled(button_pressed):
	px.show_dendrites = button_pressed

func _on_CheckBox2_toggled(button_pressed):
	px.show_dendrites_back = button_pressed

func _on_DendriteExpr_on_parsed_expression(ex):
	px.dendrite_expr = ex

func _on_NeuronExpr_on_parsed_expression(ex):
	px.neuron_expr = ex
