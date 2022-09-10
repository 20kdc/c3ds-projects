extends Control

onready var cpx_error_box: CPXErrorBox = $"%CPXErrorBox"
onready var organs: Container = $"%organs"
onready var organ_controls: Array = []
onready var faculties: Array = ($"%faculties").get_children()

var req: CPXRequest = null

func _on_VisibilityUpdateTimer_do_update():
	if req != null:
		return
	if TargetCreature.moniker == "":
		return
	req = CPXDaemon.caos_request("Organs", """
		targ mtoc \"""" + TargetCreature.moniker + """\"
		setv va00 0
		reps 9
			outv soul va00
			outs "\\n"
			addv va00 1
		repe
		outv orgn outs "\\n"
		setv va00 0
		reps orgn
			outv orgf va00 4
			outs "\\n"
			outv orgf va00 5
			outs "\\n"
			outv orgf va00 6
			outs "\\n"
			outv orgi va00 0
			outs "\\n"
			outv orgi va00 1
			outs "\\n"
			outv orgi va00 2
			outs "\\n"
			addv va00 1
		repe
	""")
	req.connect("completed", self, "_req_completed")

func _req_completed():
	cpx_error_box.update_from(req)
	if req.result_code == 0:
		var res = req.result_str().split("\n")
		# faculties
		for v in faculties:
			v.receive_faculty_update(res)
		# organs
		var ptr = 9
		var orgn = int(res[ptr])
		ptr += 1
		# ensure correct amount of organ controls (decrease)
		while len(organ_controls) > orgn:
			var last: Node = organ_controls.pop_back()
			last.queue_free()
		# ensure correct amount of organ controls (increase)
		while len(organ_controls) < orgn:
			var idx = len(organ_controls)
			var ctrl = preload("organ.tscn").instance()
			ctrl.organ_id = idx
			organs.add_child(ctrl)
			organ_controls.push_back(ctrl)
		# continue
		for idx in range(orgn):
			var ilf = float(res[ptr])
			ptr += 1
			var slf = float(res[ptr])
			ptr += 1
			var llf = float(res[ptr])
			ptr += 1
			var rcp = int(res[ptr])
			ptr += 1
			var emi = int(res[ptr])
			ptr += 1
			var rea = int(res[ptr])
			ptr += 1
			organ_controls[idx].update_status(ilf, slf, llf, rcp, emi, rea)
	req = null
