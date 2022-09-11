extends Button

func _pressed():
	$"../ScrollContainer/VBoxContainer".update_request_data(false)
