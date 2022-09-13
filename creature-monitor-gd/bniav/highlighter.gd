extends HBoxContainer

var main_brain_view: BNIAVMainBrainView

export var tag = "Agent Categories"
export var offset: int = 0
export var column = false

func _ready():
	$CatalogueLabel.tag = tag
	$CatalogueLabel.offset = offset

func _on_Button_pressed():
	if column:
		main_brain_view.highlighted_column = $CatalogueLabel.offset
	else:
		main_brain_view.highlighted_row = $CatalogueLabel.offset

