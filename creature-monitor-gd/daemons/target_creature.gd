extends Node

signal target_creature_changed()

var moniker = ""
var given_name = ""

func set_creature(x_moniker, x_given_name):
	moniker = x_moniker
	given_name = x_given_name
	emit_signal("target_creature_changed")
