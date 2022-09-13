class_name BrainNeuronSnapshot
extends Resource

# equal to index in array
export var index: int

export var input: float
export var values: PoolRealArray

func _init():
	values.resize(8)
	values.fill(0)

func import(stream: StreamPeerBuffer):
	input = stream.get_float()
	index = stream.get_32()
	for idx in range(8):
		values[idx] = stream.get_float()
