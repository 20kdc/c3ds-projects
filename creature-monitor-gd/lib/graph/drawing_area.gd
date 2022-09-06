extends Control

var parent_graph_widget: GraphWidget

func _draw():
	if parent_graph_widget != null:
		parent_graph_widget._draw_to_drawing_area()
