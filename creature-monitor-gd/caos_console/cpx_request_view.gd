extends VBoxContainer

var request_controls
# disables showing new incoming requests or hiding existing requests
var photosensitivity_lock = false

func _init():
	request_controls = {}

func _process(_delta):
	var reqs = CPXDaemon.requests
	for req in request_controls.keys():
		if (not photosensitivity_lock) and (not reqs.has(req)):
			request_controls[req].queue_free()
			request_controls.erase(req)
		else:
			var ctrl: CPXRequestViewEntry = request_controls[req]
			ctrl.update_request(req)
	if not photosensitivity_lock:
		for req in reqs.keys():
			if not request_controls.has(req):
				var ctrl: CPXRequestViewEntry = preload("cpx_request_view_entry.tscn").instance()
				add_child(ctrl)
				ctrl.update_request(req)
				request_controls[req] = ctrl
