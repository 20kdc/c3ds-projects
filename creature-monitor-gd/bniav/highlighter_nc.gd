extends HBoxContainer

var main_brain_view: BNIAVMainBrainView

export var text = "UNKNOWN"
export var offset: int = 0
export var column = false

func _ready():
	$Label.text = text

func _on_Button_pressed():
	if column:
		main_brain_view.highlighted_column = offset
	else:
		main_brain_view.highlighted_row = offset

