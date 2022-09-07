class_name GraphWidget
extends Control

onready var drawing_area = $hb/DrawingArea

func _ready():
	drawing_area.parent_graph_widget = self

func _draw_to_drawing_area():
	var rs = drawing_area.rect_size
	drawing_area.draw_rect(Rect2(Vector2.ZERO, rs - Vector2.ONE), Color.black, true)
	drawing_area.draw_rect(Rect2(Vector2.ZERO, rs - Vector2.ONE), Color.white, false)
	drawing_area.draw_line(Vector2(0, rs.y / 2), Vector2(rs.x, rs.y / 2), Color.white)
