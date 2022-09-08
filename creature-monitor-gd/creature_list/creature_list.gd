extends Control

var req: CPXRequest

onready var controls: Dictionary = {}
onready var errorbox = $CPXErrorBox
onready var entries = $sc/entries

func _ready():
	pass

func _process(_delta):
	if req != null:
		if req.poll():
			errorbox.update_from(req)
			if req.result_code == 0:
				# collate information
				var lines = req.result_str().split("\n")
				var base_size = 1
				var norn = lines[0]
				var info = {}
				# collate mapped information
				var entry_size = 6
				var ejr = range(entry_size)
				for i in range((len(lines) - base_size) / entry_size):
					var base = (i * entry_size) + base_size
					var total = []
					for j in ejr:
						total.push_back(lines[base + j])
					info[total[0]] = total
				update_ui(norn, info)
			req = null

func update_ui(norn: String, infos: Dictionary):
	# Remove old controls
	for k in controls.keys():
		if not infos.has(k):
			var c: Control = controls[k]
			c.queue_free()
			controls.erase(k)
	var names_keys = infos.keys()
	# Add new controls
	for k in names_keys:
		if not controls.has(k):
			var c: Control = preload("norn.tscn").instance()
			entries.add_child(c)
			controls[k] = c
	# Update
	for k in names_keys:
		var info = infos[k]
		controls[k].update_norn(k, norn, info)
		# Check for creature death
		if info[5] != "0":
			TargetCreature.emit_signal("creature_dead_notify", k)

func _on_VisibilityUpdateTimer_do_update():
	if req != null:
		return
	req = CPXRequest.new(CPXRequest.from_caos("""
		targ norn
		doif targ ne null
			outs gtos 0
		endi
		outs "\\n"
		enum 4 0 0
			outs gtos 0
			outs "\\n"
			outs hist name gtos 0
			outs "\\n"
			outv gnus
			outs "\\n"
			outv spcs
			outs "\\n"
			outv cage
			outs "\\n"
			outv dead
			outs "\\n"
		next
	"""))
