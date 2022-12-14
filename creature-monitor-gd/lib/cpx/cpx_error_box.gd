class_name CPXErrorBox
extends PanelContainer

func update_from(req: CPXRequest):
	visible = req.result_code != 0
	if visible:
		$vbox/hb/component.text = req.purpose
		$vbox/label.text = req.result_str()
