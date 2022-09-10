class_name CPXRequestViewEntry
extends Control

func update_request(req: CPXRequest):
	$Label.text = req.to_string()
