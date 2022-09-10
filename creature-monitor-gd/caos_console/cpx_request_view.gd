extends VBoxContainer

var request_controls
# disables showing new incoming requests or hiding existing requests
var photosensitivity_lock = false

func _init():
	request_controls = {}

func _process(_delta):
	var reqs = CPXDaemon.requests
	if not photosensitivity_lock:
		for req in request_controls.keys():
			if not reqs.has(req):
				request_controls[req].queue_free()
				request_controls.erase(req)
	for req in reqs.keys():
		var ctrl: CPXRequestViewEntry
		if not request_controls.has(req):
			if not photosensitivity_lock:
				ctrl = preload("cpx_request_view_entry.tscn").instance()
				add_child(ctrl)
				request_controls[req] = ctrl
		else:
			ctrl = request_controls[req]
		if ctrl != null:
			ctrl.update_request(req)
