class_name BrainDendriteSnapshot
extends Reference

func import(stream: StreamPeerBuffer):
	stream.get_32()
	stream.get_32()
	stream.get_32()
	stream.get_float()
	stream.get_float()
	stream.get_float()
	stream.get_float()
	stream.get_float()
	stream.get_float()
	stream.get_float()
	stream.get_float()
