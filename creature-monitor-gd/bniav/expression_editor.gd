extends VBoxContainer

signal on_parsed_expression(ex)
export var is_dendrite: bool
export var text: String

func _ready():
	$LineEdit.text = text

func _on_LineEdit_text_changed(new_text):
	var ex = Expression.new()
	var err
	if is_dendrite:
		err = ex.parse(new_text, BNIAVMainBrainView.DENDRITE_EXPR_INPUTS)
	else:
		err = ex.parse(new_text, BNIAVMainBrainView.NEURON_EXPR_INPUTS)
	if err != OK:
		$Label.visible = true
		$Label.text = ex.get_error_text()
	else:
		$Label.visible = false
	emit_signal("on_parsed_expression", ex)
