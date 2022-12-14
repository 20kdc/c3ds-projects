class_name CPXConnectorFile
extends CPXConnector

var file: File
var buffer: PoolByteArray

func _init(f: File):
	file = f

func is_connecting() -> bool:
	return false

func is_broken() -> bool:
	return (file.get_error() != OK) or not file.is_open()

func get_status() -> String:
	return "Named Pipe"

func expect(_sz: int) -> bool:
	return true

func read(amount: int) -> bool:
	if len(buffer) < amount:
		buffer.append_array(file.get_buffer(amount - len(buffer)))
	if len(buffer) >= amount:
		last_read = buffer.subarray(0, amount - 1)
		if amount == len(buffer):
			buffer = PoolByteArray()
		else:
			buffer = buffer.subarray(amount, len(buffer) - 1)
		return true
	return false

func write(data: PoolByteArray) -> int:
	file.store_data(data)
	file.flush()
	return OK

func close():
	file.close()
