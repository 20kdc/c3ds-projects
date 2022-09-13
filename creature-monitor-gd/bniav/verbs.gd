extends VBoxContainer

onready var main_brain_view: BNIAVMainBrainView = $"%main_brain_view"

export var tag = "Agent Categories"
export var column = false
export var count = 40

# Declare member variables here. Examples:
# var a = 2
# var b = "text"


# Called when the node enters the scene tree for the first time.
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

# Called every frame. 'delta' is the elapsed time since the previous frame.
#func _process(delta):
#	pass
