class_name CPXRequestViewEntry
extends Control

var req: CPXRequest

func update_request(r: CPXRequest):
	req = r
	$HBoxContainer/Label.text = req.to_string()

func _on_Button_pressed():
	if req != null:
		req.terminate("client: forcibly terminated by user")
