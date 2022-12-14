# Exists as part of adding the second connection method
class_name CPXConnector
extends Reference

var last_read: PoolByteArray

func is_connecting() -> bool:
	return true

func is_broken() -> bool:
	return true

func get_status() -> String:
	return "STATUS NYI"

# Workaround to stop hanging in early days
# Always return true if you don't care ig
func expect(_amount: int) -> bool:
	return false

# Buffers until reached and then returns true.
func read(_amount: int) -> bool:
	return true

func write(_data: PoolByteArray) -> int:
	return ERR_BUG

func close():
	pass
