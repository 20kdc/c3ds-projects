class_name BrainNeuronSnapshot
extends Resource

# equal to index in array
export var index: int

export var input: float
export var values: PoolRealArray

# lobe width is passed in during import for these, for ease of access
export var x: int
export var y: int

func _init():
	values.resize(8)
	values.fill(0)

func import(stream: StreamPeerBuffer, parent_w: int):
	input = stream.get_float()
	index = stream.get_32()
	x = index % parent_w
	y = index / parent_w
	for idx in range(8):
		values[idx] = stream.get_float()
