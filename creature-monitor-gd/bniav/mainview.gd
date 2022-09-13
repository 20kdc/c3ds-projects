extends Control

const CELL_SIZE = Vector2(16, 16)

var snapshot: BrainSnapshot
var relative_canvas_nu: Rect2

func _ready():
	update()

func _draw():
	var bs = Rect2(Vector2.ZERO, rect_size)
	draw_rect(bs, Color.black)
	if snapshot != null:
		var cfg = snapshot.cfg()
		for idx in cfg.lobe_range:
			var lobe = snapshot.lobe(idx)
			var lobe_rect = lobe.as_rect()
			var lobe_rect_translated = Rect2(lobe_rect.position - relative_canvas_nu.position, lobe_rect.size)
			var lobe_rect_scaled = Rect2(lobe_rect_translated.position * CELL_SIZE, lobe_rect_translated.size * CELL_SIZE)
			draw_rect(lobe_rect_scaled, Color.white, false)

func _on_Brain_snapshot_updated(sn):
	snapshot = sn
	var irect = snapshot.as_rect()
	relative_canvas_nu = irect.grow(1)
	rect_min_size = relative_canvas_nu.size * CELL_SIZE
	update()
