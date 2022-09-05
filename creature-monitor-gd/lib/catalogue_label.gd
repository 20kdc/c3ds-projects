class_name CatalogueLabel
extends Label

export var tag: String setget _set_tag
export var offset: int setget _set_offset

func _ready():
	CatalogueTracker.connect("cache_updated", self, "_dirty_cache")

func _dirty_cache():
	set_process(true)

func _set_tag(value: String):
	tag = value
	set_process(true)

func _set_offset(value: int):
	offset = value
	set_process(true)

func _process(_delta):
	if is_visible_in_tree():
		text = CatalogueTracker.lookup(tag, offset)
		set_process(false)
