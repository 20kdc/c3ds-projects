class_name BrainLobeSnapshot
extends Reference

var name: String
var ruleset_init: BrainRuleset
var ruleset_update: BrainRuleset
var x: int
var y: int
var w: int
var h: int
var neurons: Array

func _init():
	ruleset_init = BrainRuleset.new()
	ruleset_update = BrainRuleset.new()

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
		neuron.import(stream)
	# random footer
	stream.get_data(10)
