class_name GraphWidget
extends Control

onready var drawing_area: GraphViewport = $hb/DrawingArea
onready var y_zoom = find_node("y_zoom")
onready var y_offset = find_node("y_offset")
onready var time_range = find_node("time_range")
onready var lb_top = find_node("lb_top")
onready var lb_mid = find_node("lb_mid")
onready var lb_bot = find_node("lb_bot")

func _ready():
	drawing_area.clear_points()
	drawing_area.add_point(200, 0)
	drawing_area.add_point(225, 1)
	drawing_area.add_point(240, -1)
	drawing_area.add_point(250, 0)
	drawing_area.add_point(300, -1)
	_forward_settings_to_drawing_area()

func clear_points():
	drawing_area.clear_points()

func add_point(time: float, value: float):
	drawing_area.add_point(time, value)

func _forward_settings_to_drawing_area():
	var y_half = pow(2.0, y_zoom.value)
	var yt = y_offset.value + y_half
	var yb = y_offset.value - y_half
	drawing_area.y_top = yt
	drawing_area.y_bottom = yb
	drawing_area.time_range = time_range.value
	y_offset.step = y_half / 4.0
	lb_top.text = str(yt)
	lb_mid.text = str((yt + yb) / 2)
	lb_bot.text = str(yb)

func _on_y_offset_value_changed(value):
	_forward_settings_to_drawing_area()

func _on_y_zoom_value_changed(value):
	_forward_settings_to_drawing_area()

func _on_time_range_value_changed(value):
	_forward_settings_to_drawing_area()

func _on_Button_pressed():
	time_range.value = 100
	y_offset.value = 0
	y_zoom.value = 0
	_forward_settings_to_drawing_area()
