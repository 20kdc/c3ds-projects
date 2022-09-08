extends Button

var req: CPXRequest

export var caos = ""
export var caos_release = ""

func _process(_delta):
	if req != null:
		if req.poll():
			req = null

func _pressed():
	if not toggle_mode:
		if req == null:
			req = CPXRequest.new(CPXRequest.from_caos(caos))

func _toggled(button_pressed):
	if toggle_mode:
		if req == null:
			if button_pressed:
				req = CPXRequest.new(CPXRequest.from_caos(caos))
			else:
				req = CPXRequest.new(CPXRequest.from_caos(caos_release))
