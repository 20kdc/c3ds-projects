extends Node

var config: BrainConfig
var snapshot: BrainSnapshot
var req: CPXRequest
var file_view: bool = false

onready var source_of_data_label = find_node("source_of_data")

signal snapshot_updated(snapshot)

func _ready():
	TargetCreature.connect("target_creature_changed", self, "_invalidate_config")
	_update_source_of_data_label()

func _invalidate_config():
	config = null

func _on_VisibilityUpdateTimer_do_update():
	if req != null:
		return
	if file_view:
		return
	var mon = TargetCreature.moniker
	if mon == "":
		return
	if config == null:
		req = CPXDaemon.caos_request("Brain Config", BrainConfig.snapshot_caos(mon))
		req.connect("completed", self, "_completed_config")
	else:
		req = CPXDaemon.caos_request("Brain Snapshot", BrainSnapshot.snapshot_caos(config, mon))
		req.connect("completed", self, "_completed_snapshot")

func _completed_config():
	if req.result_code == 0:
		config = BrainConfig.new()
		config.import(req)
	_completed_gen()

func _completed_snapshot():
	if req.result_code == 0 and not file_view:
		if config != null:
			snapshot = BrainSnapshot.new()
			snapshot.import(config, req)
			emit_signal("snapshot_updated", snapshot)
	_completed_gen()

func _completed_gen():
	$VBoxContainer/CPXErrorBox.update_from(req)
	req = null

func _on_snapload_pressed():
	snapshot = load("user://snapshot.tres")
	$VBoxContainer/CPXErrorBox.visible = false
	file_view = true
	emit_signal("snapshot_updated", snapshot)
	_update_source_of_data_label()

func _on_snapunload_pressed():
	file_view = false
	_update_source_of_data_label()

func _on_snapsave_pressed():
	print("SNAPSHOT SAVE: " + OS.get_user_data_dir())
	if snapshot != null:
		ResourceSaver.save("user://snapshot.tres", snapshot)

func _update_source_of_data_label():
	if snapshot == null:
		source_of_data_label.text = "None"
	elif file_view:
		source_of_data_label.text = "Snapshot"
	else:
		source_of_data_label.text = "Live"
