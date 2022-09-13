extends Node

onready var px: BNIAVMainBrainView = get_parent()

func _on_Brain_snapshot_updated(snapshot):
	px.snapshot = snapshot

