extends Button

var main_brain_view: BNIAVMainBrainView
export var column = false

func _ready():
	text = "Clear"

func _pressed():
	if column:
		main_brain_view.highlighted_column = -1
	else:
		main_brain_view.highlighted_row = -1

