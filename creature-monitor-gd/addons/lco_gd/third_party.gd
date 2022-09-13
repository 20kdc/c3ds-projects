# This is a clone of Godot's "Third-party Licenses" panel.
tool
class_name LCOGDThirdParty
extends HSplitContainer

onready var map = {}

var tree: Tree
var text: TextEdit

func _ready():
	tree = Tree.new()
	tree.name = "Tree"
	tree.hide_root = true
	tree.connect("item_selected", self, "_on_Tree_item_selected")
	add_child(tree)
	text = TextEdit.new()
	text.name = "Text"
	text.readonly = true
	add_child(text)
	text.add_color_override("font_color_readonly", text.get_color("font_color"))

	var root = tree.create_item()
	var all_components = tree.create_item(root)
	map[all_components] = gen_all_components()
	all_components.set_text(0, "All Components")
	var components = tree.create_item(root)
	map[components] = "Please see an individual component in this group."
	components.set_text(0, "Components")
	var cpr = Engine.get_copyright_info()
	for comp in cpr:
		var item = tree.create_item(components)
		item.set_text(0, comp["name"])
		map[item] = gen_component(comp)
	var licenses = tree.create_item(root)
	map[licenses] = "Please see an individual license in this group."
	licenses.set_text(0, "Licenses")
	var x = Engine.get_license_info()
	for v in x:
		var item = tree.create_item(licenses)
		item.set_text(0, v)
		map[item] = x[v]

func gen_component(comp):
	var total = ""
	total += str(comp["name"]) + "\n"
	for v2 in comp["parts"]:
		total += "\n"
		total += "\tFiles:\n"
		for v3 in v2["files"]:
			total += "\t\t" + v3 + "\n"
		for v3 in v2["copyright"]:
			total += "\t(c) " + v3 + "\n"
		total += "\tLicense: " + v2["license"] + "\n"
	return total

static func gen_all_components():
	var cpr = Engine.get_copyright_info()
	var total = ""
	for comp in cpr:
		total += "- " + str(comp["name"]) + "\n"
		for v2 in comp["parts"]:
			total += "\n"
			for v3 in v2["copyright"]:
				total += "\t(c) " + v3 + "\n"
			total += "\tLicense: " + v2["license"] + "\n"
		total += "\n"
	
	var x = Engine.get_license_info()
	for v in x:
		total += "- " + v + "\n\n"
		var ltx: String = x[v]
		for v2 in ltx.split("\n"):
			total += "\t" + v2 + "\n"
	return total

func _on_Tree_item_selected():
	var itm = tree.get_selected()
	if map.has(itm):
		text.text = map[itm]
