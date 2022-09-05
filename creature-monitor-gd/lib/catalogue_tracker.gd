extends Node

var req: CPXRequest
var req_cache_id = ""
onready var catalogue_cache = {}
onready var catalogue_queued = {}
onready var catalogue_lookup_queue_commands = []
onready var catalogue_lookup_queue_ids = []

func _process(_delta):
	if req != null:
		if req.poll():
			if req.result_code == 0:
				catalogue_cache[req_cache_id] = req.result_str()
			req = null
			check_lookup_queue()

func check_lookup_queue():
	if req != null:
		return
	if len(catalogue_lookup_queue_commands) > 0:
		var cmd = catalogue_lookup_queue_commands.pop_front()
		req = CPXRequest.new(CPXRequest.from_caos(cmd))
		req_cache_id = catalogue_lookup_queue_ids.pop_front()

func lookup(name: String, index: int):
	var cache_id = name + ":" + str(index)
	if catalogue_cache.has(cache_id):
		return catalogue_cache[cache_id]
	if catalogue_queued.has(cache_id):
		return cache_id
	catalogue_lookup_queue_commands.push_back("outs read \"" + name + "\" " + str(index))
	catalogue_lookup_queue_ids.push_back(cache_id)
	catalogue_queued[cache_id] = true
	check_lookup_queue()
	return cache_id
