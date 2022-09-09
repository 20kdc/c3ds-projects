class_name CAOSMacroButton
extends Button

var req: CPXRequest

export var caos = ""
export var caos_release = ""
export var prefix_targ_creature = false

func _process(_delta):
	if req != null:
		if req.poll():
			req = null

func _run_caos(tc: String):
	if prefix_targ_creature:
		var moniker = TargetCreature.moniker
		if moniker == "":
			return false
		tc = "targ mtoc \"" + moniker + "\"\n" + tc
	if req != null:
		return false
	req = CPXRequest.new(CPXRequest.from_caos(tc))
	return true

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
