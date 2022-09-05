extends Control

onready var map = {}

func _ready():
	var root = $Tree.create_item()
	var all_components = $Tree.create_item(root)
	map[all_components] = gen_all_components()
	all_components.set_text(0, "All Components")
	var components = $Tree.create_item(root)
	map[components] = "Please see an individual component in this group."
	components.set_text(0, "Components")
	var cpr = Engine.get_copyright_info()
	for comp in cpr:
		var item = $Tree.create_item(components)
		item.set_text(0, comp["name"])
		map[item] = gen_component(comp)
	var licenses = $Tree.create_item(root)
	map[licenses] = "Please see an individual license in this group."
	licenses.set_text(0, "Licenses")
	var x = Engine.get_license_info()
	for v in x:
		var item = $Tree.create_item(licenses)
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

func gen_all_components():
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
	var itm = $Tree.get_selected()
	if map.has(itm):
		$Text.text = map[itm]
