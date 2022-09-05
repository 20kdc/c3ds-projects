class_name VisibilityUpdateTimer
extends Node

signal do_update()

onready var parent_ci: CanvasItem = get_parent()

export var interval = 1.0

var time_counter = 0.0
var was_visible_in_tree = false

func _process(delta):
	if parent_ci.is_visible_in_tree():
		if not was_visible_in_tree:
			emit_signal("do_update")
		was_visible_in_tree = true
		time_counter += delta
		if time_counter >= interval:
			# we don't need it to be that precise
			time_counter = 0.0
			emit_signal("do_update")
	else:
		time_counter = 0.0
		was_visible_in_tree = false
