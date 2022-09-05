extends VBoxContainer

onready var rtl: RichTextLabel = $RichTextLabel

var current_request: CPXRequest

func _ready():
	pass # Replace with function body.

func _process(_delta):
	if current_request != null:
		if current_request.poll():
			var text = current_request.result.get_string_from_ascii()
			text = text.strip_edges()
			if current_request.result_code != 0:
				rtl.push_color(Color.tomato)
				rtl.push_italics()
				rtl.add_text(text)
				rtl.pop()
				rtl.pop()
			else:
				rtl.push_color(Color.white)
				rtl.add_text(text)
				rtl.pop()
			rtl.newline()
			current_request = null

func _on_TextEdit_text_entered(new_text):
	if current_request != null:
		return
	var req = ("execute\n" + new_text + "\u0000").to_utf8()
	rtl.push_color(Color.blanchedalmond)
	rtl.add_text("> " + new_text)
	rtl.pop()
	rtl.newline()
	current_request = CPXRequest.new(req)
