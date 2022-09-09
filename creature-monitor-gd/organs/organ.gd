extends Control

export var organ_id = -1

onready var kill_btn = $kill
onready var heal_btn = $heal

func _ready():
	kill_btn.prefix_targ_creature = true
	kill_btn.caos = _gen_caos(0)
	heal_btn.prefix_targ_creature = true
	heal_btn.caos = _gen_caos(1000000)

func _on_Label2_toggled(button_pressed):
	kill_btn.disabled = not button_pressed

func update_status(ilf: float, slf: float, llf: float, rcp: int, emi: int, rea: int):
	var status = ""
	if llf < 0.5:
		status = " FAILED"
	var stats = "Rcp " + str(rcp) + ", Emi " + str(emi) + ", Rea " + str(rea)
	$Label.text = "Organ " + str(organ_id) + status + "\n" + stats
	$lf/pslf.value = slf / ilf
	$lf/pllf.value = llf / ilf
	$lf/lslf.text = str(slf)
	$lf/lllf.text = str(llf)

func _gen_caos(target: float) -> String:
	var restore_chems = """
	sets va02 caos 1 1 0 0 va01 1 0 va02
	"""
	return """
	setv va00 0
	sets va01 ""
	reps 256
		adds va01 "chem "
		adds va01 vtos va00
		adds va01 " -1 "
		adds va01 "chem "
		adds va01 vtos va00
		adds va01 " "
		adds va01 vtos chem va00
		adds va01 " "
		addv va00 1
	repe

	reps 8192
		""" + restore_chems + """
		chem 34 1
		setv va03 orgf """ + str(organ_id) + """ 5
		subv va03 """ + str(target) + """
		injr """ + str(organ_id) + """ va03
		step 4
	repe
	""" + restore_chems + """
	"""
