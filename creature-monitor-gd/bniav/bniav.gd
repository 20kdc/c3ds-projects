extends Node

var config: BrainConfig
var snapshot: BrainSnapshot
var req: CPXRequest

signal snapshot_updated(snapshot)

func _ready():
	TargetCreature.connect("target_creature_changed", self, "_invalidate_config")

func _invalidate_config():
	config = null

func _on_VisibilityUpdateTimer_do_update():
	if req != null:
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
	if req.result_code == 0:
		if config != null:
			snapshot = BrainSnapshot.new()
			snapshot.import(config, req)
			emit_signal("snapshot_updated", snapshot)
	_completed_gen()

func _completed_gen():
	$VBoxContainer/CPXErrorBox.update_from(req)
	req = null
