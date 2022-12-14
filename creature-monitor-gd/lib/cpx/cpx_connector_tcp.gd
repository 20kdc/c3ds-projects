class_name CPXConnectorTCP
extends CPXConnector

var peer: StreamPeerTCP
var make_broken: bool = false

func _init(sp: StreamPeerTCP):
	peer = sp

func is_connecting() -> bool:
	return peer.get_status() == StreamPeerTCP.STATUS_CONNECTING

func is_broken() -> bool:
	if make_broken:
		return true
	return peer.get_status() == StreamPeerTCP.STATUS_ERROR

func get_status() -> String:
	if peer.is_connected_to_host():
		return "TCP, AVB " + str(peer.get_available_bytes())
	else:
		return "TCP"

func expect(sz: int) -> bool:
	return peer.get_available_bytes() >= sz

func read(amount: int) -> bool:
	var res = peer.get_data(amount)
	last_read = res[1]
	if res[0] == OK:
		return true
	else:
		make_broken = true
		return false

func write(data: PoolByteArray) -> int:
	peer.set_no_delay(true)
	return peer.put_data(data)

func close():
	if peer.is_connected_to_host():
		peer.disconnect_from_host()
