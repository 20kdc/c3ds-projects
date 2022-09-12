class_name BrainTractSnapshot
extends Reference

var src_lobe: int
var src_min: int

var dst_lobe: int
var dst_min: int

var ruleset_init: BrainRuleset
var ruleset_update: BrainRuleset

var dendrites: Array

func _init():
	ruleset_init = BrainRuleset.new()
	ruleset_update = BrainRuleset.new()

func import(stream: StreamPeerBuffer):
	stream.get_32()
	stream.get_32()
	src_lobe = stream.get_32()
	src_min = stream.get_32()
	stream.get_32()
	stream.get_8()
	dst_lobe = stream.get_32()
	dst_min = stream.get_32()
	stream.get_32()
	stream.get_8()
	stream.get_8()
	stream.get_8()
	# rulesets
	ruleset_init.import(stream)
	ruleset_update.import(stream)
	# dendrites
	var dcount = stream.get_32()
	dendrites = []
	for _idx in range(dcount):
		var dendrite = BrainDendriteSnapshot.new()
		dendrites.push_back(dendrite)
		dendrite.import(stream)
	# random footer
	stream.get_data(10)
