class_name BrainRuleset
extends Resource

func import(stream: StreamPeerBuffer):
	# don't REALLY need to store this
	for _i in range(16):
		stream.get_32()
		stream.get_32()
		stream.get_32()
		stream.get_float()
