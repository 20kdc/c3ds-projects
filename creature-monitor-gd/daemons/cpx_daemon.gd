extends Node

var requests: Array

func _init():
	requests = []

func _process(_delta):
	# Process requests in sequence
	# They used to be processed in parallel, but with named pipe stuff...
	while len(requests) > 0:
		var req: CPXRequest = requests[0]
		if req.poll():
			requests.erase(req)
		else:
			break

func cpx_request(purpose: String, pba: PoolByteArray) -> CPXRequest:
	var req = CPXRequest.new(purpose, pba)
	requests.push_back(req)
	return req

func cpx_string_request(purpose: String, text: String) -> CPXRequest:
	var pba = text.to_utf8()
	pba.push_back(0)
	return cpx_request(purpose, pba)

func caos_request(purpose: String, text: String) -> CPXRequest:
	return cpx_string_request(purpose, "execute\n" + text)
