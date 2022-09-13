extends VBoxContainer

onready var main_brain_view: BNIAVMainBrainView = $"%main_brain_view"

export var tag = "Agent Categories"
export var column = false
export var count = 40

func _ready():
	var ch = preload("clear_highlight.gd").new()
	ch.main_brain_view = main_brain_view
	ch.column = column
	add_child(ch)
	for idx in range(count):
		var h = preload("highlighter.tscn").instance()
		h.main_brain_view = main_brain_view
		h.tag = tag
		h.offset = idx
		h.column = column
		add_child(h)
