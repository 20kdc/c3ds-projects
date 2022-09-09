extends CAOSMacroButton

var faculty_id: int = 0

func _ready():
	faculty_id = int(name)
	caos = "soul " + str(faculty_id) + " 1"
	caos_release = "soul " + str(faculty_id) + " 0"
	prefix_targ_creature = true

func receive_faculty_update(faculties: PoolStringArray):
	set_pressed_no_signal(faculties[faculty_id] != "0")
