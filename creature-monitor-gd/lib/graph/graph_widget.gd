class_name GraphWidget
extends Control

onready var drawing_area = $hb/DrawingArea

func _ready():
	drawing_area.parent_graph_widget = self

func _draw_to_drawing_area():
	drawing_area.draw_line(Vector2.ZERO, Vector2.ONE * 32, Color.white)
	drawing_area.draw_rect(Rect2(Vector2.ZERO, drawing_area.rect_size - Vector2.ONE), Color.white, false)
