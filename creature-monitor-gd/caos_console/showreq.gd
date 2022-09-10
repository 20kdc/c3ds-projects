extends Button

func _toggled(button_pressed):
	$"../ScrollContainer".visible = button_pressed
	$"../ScrollContainer/VBoxContainer".photosensitivity_lock = button_pressed
