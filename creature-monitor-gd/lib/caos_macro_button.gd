extends Button

var req: CPXRequest

export var caos = ""

func _process(_delta):
	if req != null:
		if req.poll():
			req = null

func _pressed():
	if req == null:
		req = CPXRequest.new(CPXRequest.from_caos(caos))
