class_name BrainLobeSnapshot
extends Resource

export var name: String

export var x: int
export var y: int
export var w: int
export var h: int

# Need to figure out what to do about the native type export restriction...
export var ruleset_init: Resource
export var ruleset_update: Resource

# of BrainNeuronSnapshot
export var neurons: Array

func _init():
	ruleset_init = BrainRuleset.new()
	ruleset_update = BrainRuleset.new()

func neuron(i: int) -> BrainNeuronSnapshot:
	return neurons[i]

func as_rect() -> Rect2:
	return Rect2(x, y, w, h)
func neuron_as_rect(n: int) -> Rect2:
	return Rect2(x + (n % w), y + (n / w), 1, 1)

func import(stream: StreamPeerBuffer):
	stream.get_32()
	stream.get_32()
	var name_array: PoolByteArray = stream.get_data(4)[1]
	name = name_array.get_string_from_ascii()
	stream.get_32()
	# xywh
	x = stream.get_32()
	y = stream.get_32()
	w = stream.get_32()
	h = stream.get_32()
	# blah
	stream.get_32()
	stream.get_32()
	stream.get_32()
	stream.get_32()
	# rulesets
	ruleset_init.import(stream)
	ruleset_update.import(stream)
	# neurons
	neurons = []
	for _idx in range(w * h):
		var neuron = BrainNeuronSnapshot.new()
		neurons.push_back(neuron)
		neuron.import(stream, w)
	# random footer
	stream.get_data(10)
