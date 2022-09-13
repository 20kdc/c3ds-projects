class_name BNIAVMainBrainView
extends Control

const CELL_SIZE = Vector2(16, 16)

var snapshot: BrainSnapshot setget set_snapshot
var relative_canvas_nu: Rect2

var highlighted_row = -1 setget set_highlighted_row
var highlighted_column = -1 setget set_highlighted_column

var show_dendrites = true setget set_show_dendrites
var show_dendrites_back = true setget set_show_dendrites_back

func _ready():
	update()

func set_highlighted_row(c: int):
	highlighted_row = c
	update()

func set_highlighted_column(c: int):
	highlighted_column = c
	update()

func _draw():
	var font = get_font("font", "Button")
	var bs = Rect2(Vector2.ZERO, rect_size)
	draw_rect(bs, Color.black)
	if snapshot != null:
		var cfg = snapshot.cfg()
		for idx in cfg.lobe_range:
			var lobe = snapshot.lobe(idx)
			var lobe_rect = lobe.as_rect()
			var lobe_rect_scaled = translate_and_scale(lobe_rect)
			for n in lobe.neurons:
				var neuron: BrainNeuronSnapshot = n
				var nr = lobe.neuron_as_rect(neuron.index)
				var nrs = translate_and_scale(nr)
				if highlighted_column == neuron.x or highlighted_row == neuron.y:
					draw_rect(nrs, Color(0.3, 0.3, 0.3), true)
				var nrs_dg = nrs.grow(-4)
				draw_rect(nrs_dg, neuron_to_colour(neuron.values[0]), true)
			# lobe details
			var base_text_pos = lobe_rect_scaled.position + Vector2(0, -4)
			for cidx in range(4):
				base_text_pos.x += draw_char(font, base_text_pos, lobe.name.substr(cidx, 1), "")
			draw_rect(lobe_rect_scaled, Color.white, false)
		if show_dendrites:
			for idx in cfg.tract_range:
				var tract = snapshot.tract(idx)
				var lobe_src = snapshot.lobe(tract.src_lobe)
				var lobe_dst = snapshot.lobe(tract.dst_lobe)
				for d in tract.dendrites:
					var dendrite: BrainDendriteSnapshot = d
					var neuron_src_rect = lobe_src.neuron_as_rect(dendrite.src_neuron)
					var neuron_dst_rect = lobe_dst.neuron_as_rect(dendrite.dst_neuron)
					var src_pt = translate_and_scale(neuron_src_rect).get_center()
					var dst_pt = translate_and_scale(neuron_dst_rect).get_center()
					var weight = dendrite.values[0]
					var value = lobe_src.neuron(dendrite.src_neuron).values[0]
					if show_dendrites_back:
						draw_line(src_pt, dst_pt, Color(0.5, 0.5, 0.5), 2, true)
					draw_line(src_pt, dst_pt, neuron_to_colour(weight * value))

func neuron_to_colour(f: float) -> Color:
	return Color(f * -16, abs(f), f * 16)

func translate_and_scale(r: Rect2) -> Rect2:
	var t = Rect2(r.position - relative_canvas_nu.position, r.size)
	var s = Rect2(t.position * CELL_SIZE, t.size * CELL_SIZE)
	return s

func set_snapshot(sn):
	snapshot = sn
	var irect = snapshot.as_rect()
	relative_canvas_nu = irect.grow(1)
	rect_min_size = relative_canvas_nu.size * CELL_SIZE
	update()

func set_show_dendrites(button_pressed):
	show_dendrites = button_pressed
	update()

func set_show_dendrites_back(button_pressed):
	show_dendrites_back = button_pressed
	update()
