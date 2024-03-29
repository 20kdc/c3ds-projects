extends VBoxContainer

onready var main_brain_view: BNIAVMainBrainView = $"%main_brain_view"

export var entries: PoolStringArray
export var column = false

func _ready():
	var ch = preload("clear_highlight.gd").new()
	ch.main_brain_view = main_brain_view
	ch.column = column
	add_child(ch)
	var idx = 0
	for text in entries:
		var h = preload("highlighter_nc.tscn").instance()
		h.main_brain_view = main_brain_view
		h.text = text
		h.offset = idx
		h.column = column
		add_child(h)
		idx += 1

