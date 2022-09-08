class_name GraphLine
extends Reference

var _data_points
signal updated()

func _init():
	_data_points = []

func clear():
	_data_points.clear()
	emit_signal("updated")

func add(time: float, value: float):
	if len(_data_points) > 0:
		var ldp: Vector2 = _data_points[len(_data_points) - 1]
		# don't allow disordered points, ensure forward progress
		if ldp.x >= time:
			return
	_data_points.push_back(Vector2(time, value))
	emit_signal("updated")

func point(idx: int) -> Vector2:
	return _data_points[idx]

func points() -> int:
	return len(_data_points)

func latest() -> Vector2:
	return _data_points[len(_data_points) - 1]

func crop_to(time_range: float):
	# clean older points
	while len(_data_points) > 2:
		var l: Vector2 = latest()
		var cutoff = l.x - time_range
		if _data_points[0].x < cutoff:
			_data_points.pop_front()
		else:
			break
	emit_signal("updated")

func array() -> Array:
	return _data_points
