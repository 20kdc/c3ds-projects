class_name BrainDendriteSnapshot
extends Resource

# equal to index in array
export var index: int

# Source/destination lobe is defined by tract
export var srcNeuron: int
export var dstNeuron: int

export var values: PoolRealArray

func _init():
	values.resize(8)
	values.fill(0)

func import(stream: StreamPeerBuffer):
	index = stream.get_32()
	srcNeuron = stream.get_32()
	dstNeuron = stream.get_32()
	for idx in range(8):
		values[idx] = stream.get_float()
