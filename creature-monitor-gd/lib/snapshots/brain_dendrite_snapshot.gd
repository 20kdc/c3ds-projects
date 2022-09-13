class_name BrainDendriteSnapshot
extends Resource

# equal to index in array
export var index: int

# Source/destination lobe is defined by tract
export var src_neuron: int
export var dst_neuron: int

export var values: PoolRealArray

func _init():
	values.resize(8)
	values.fill(0)

func import(stream: StreamPeerBuffer):
	index = stream.get_32()
	src_neuron = stream.get_32()
	dst_neuron = stream.get_32()
	for idx in range(8):
		values[idx] = stream.get_float()
