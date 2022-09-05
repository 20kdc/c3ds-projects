extends Node

signal target_creature_changed()

var target_creature_moniker = ""
var target_creature_name = ""

func set_current_creature(moniker, name):
	target_creature_moniker = moniker
	target_creature_name = name
	emit_signal("target_creature_changed")
