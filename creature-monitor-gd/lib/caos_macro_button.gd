class_name CAOSMacroButton
extends Button

var req: CPXRequest

export var caos = ""
export var caos_release = ""
export var prefix_targ_creature = false

func _run_caos(tc: String):
	if prefix_targ_creature:
		var moniker = TargetCreature.moniker
		if moniker == "":
			return false
		tc = "targ mtoc \"" + moniker + "\"\n" + tc
	if req != null:
		return false
	req = CPXDaemon.caos_request("Macro", tc)
	req.connect("completed", self, "_req_completed")
	return true

func _req_completed():
	print(req.result_str())
	req = null

func _pressed():
	if not toggle_mode:
		_run_caos(caos)

func _toggled(button_pressed):
	if toggle_mode:
		if button_pressed:
			if not _run_caos(caos):
				call_deferred("set_pressed_no_signal", false)
		else:
			if not _run_caos(caos_release):
				call_deferred("set_pressed_no_signal", true)
