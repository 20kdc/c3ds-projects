extends Control

func _ready():
	update()

func _draw():
	var bs = Rect2(Vector2.ZERO, rect_size)
	draw_rect(bs, Color.black)
