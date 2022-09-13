# use as follows:
# TARGET_FILE=../COPYING-godot.txt godot --path my-project -s "addons/lco_gd/gen_gdcf.gd"
extends MainLoop

func _initialize():
	# presumably, would do something here
	var content = ""
	content += LCOGDThirdParty.gen_all_components()
	var f = File.new()
	f.open(OS.get_environment("TARGET_FILE"), File.WRITE)
	f.store_string(content.replace("\n", "\r\n"))
	f.close()

func _idle(_delta):
	return true
