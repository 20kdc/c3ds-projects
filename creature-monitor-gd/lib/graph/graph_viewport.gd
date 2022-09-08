class_name GraphViewport
extends Control

var _data_points: GraphLine

export var time_range: float = 100 setget set_time_range
export var y_top: float = 1 setget set_y_top
export var y_bottom: float = -1 setget set_y_bottom

func _init():
	_data_points = GraphLine.new()

func clear_points():
	_data_points.clear()
	update()

func add_point(time: float, value: float):
	_data_points.add(time, value)
	_data_points.crop_to(time_range)
	update()

func _get_dp_transform():
	var rs = rect_size
	var latest_point: Vector2 = _data_points.latest()
	# print(latest_point)
	var sc = y_bottom - y_top
	var p_ofs = Vector2(time_range - latest_point.x, -y_top)
	var p_mul = Vector2(rs.x / time_range, rs.y / sc)
	return Transform2D.IDENTITY.scaled(p_mul).translated(p_ofs)

func _draw():
	var rs = rect_size
	var main_rect = Rect2(Vector2(1, 0), rs - Vector2.ONE)
	draw_rect(main_rect, Color.black, true)
	draw_line(Vector2(0, rs.y / 2), Vector2(rs.x, rs.y / 2), Color.white)

	if _data_points.points() > 0:
		var dp = _get_dp_transform()

		var has_last_point = false
		var last_point: Vector2 = Vector2.ZERO
		for v in _data_points.array():
			var this_point: Vector2 = v
			var this_point_xf = dp.xform(this_point)
			draw_line(Vector2(this_point_xf.x, 0), Vector2(this_point_xf.x, rs.y), Color(0.125, 0.125, 0.125))
			if has_last_point:
				draw_line(dp.xform(last_point), this_point_xf, Color.green, 0.5, true)
			last_point = this_point
			has_last_point = true

	# draw line overlay
	draw_rect(main_rect, Color.white, false)

func set_time_range(value):
	time_range = value
	update()

func set_y_top(value):
	y_top = value
	update()

func set_y_bottom(value):
	y_bottom = value
	update()
