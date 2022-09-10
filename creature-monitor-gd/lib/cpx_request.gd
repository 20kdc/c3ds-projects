class_name CPXRequest
extends Reference

const STATE_INIT = 0
const STATE_CONNECTING = 1
const STATE_READBACK = 2
const STATE_FINISHED = 3

var purpose: String
var spt: StreamPeerTCP
var request: PoolByteArray
var result: PoolByteArray
var result_code: int = 0
var result_error_internal: bool = false
var result_read_remainder: int = 0
var state: int = STATE_INIT

signal completed()

# WARNING: Don't create directly, use CPXDaemon
func _init(s: String, r: PoolByteArray):
	purpose = s
	request = r

func to_string() -> String:
	var state_str = "unknown state"
	if state == STATE_INIT:
		state_str = "INIT"
	elif state == STATE_CONNECTING:
		state_str = "CONNECTING"
	elif state == STATE_READBACK:
		state_str = "READBACK"
	elif state == STATE_FINISHED:
		state_str = "FINISHED"
	return purpose + ": " + state_str

func result_str() -> String:
	var tmp: PoolByteArray = result
	if len(tmp) > 0:
		if tmp[len(tmp) - 1] == 0:
			tmp.remove(len(tmp) - 1)
	return tmp.get_string_from_ascii()

func _internal_error(text: String):
	result = text.to_utf8()
	result.push_back(0)
	result_code = 2
	result_error_internal = true
	_finish_closeoff()

func _finish_metadata():
	if result_code != 0:
		if result_str().begins_with("caosprox: "):
			result_error_internal = true
	_finish_closeoff()

func _finish_closeoff():
	state = STATE_FINISHED
	if spt.is_connected_to_host():
		spt.disconnect_from_host()
	spt = null
	emit_signal("completed")

# True == done!
func poll() -> bool:
	# Deliberate fallthrough
	if state == STATE_FINISHED:
		return true
	if state == STATE_INIT:
		spt = StreamPeerTCP.new()
		spt.big_endian = false
		# NOTE: Don't make this "localhost", it doesn't work on Windows
		if spt.connect_to_host("127.0.0.1", 19960) != OK:
			_internal_error("client: failed to open connection - run caosprox.exe!")
			return true
		spt.put_32(len(request))
		if spt.put_data(request) != OK:
			_internal_error("client: failed to write request - run caosprox.exe!")
			return true
		state = STATE_CONNECTING
	if state == STATE_CONNECTING:
		if spt.get_available_bytes() >= 48:
			var res = spt.get_data(48)
			var res_err = res[0]
			var res_data: PoolByteArray = res[1]
			if res_err != OK:
				_internal_error("client: could not get headers")
				return true
			else:
				var data_stream_peer = StreamPeerBuffer.new()
				data_stream_peer.data_array = res_data
				data_stream_peer.big_endian = false
				data_stream_peer.seek(24 + 8)
				result_code = data_stream_peer.get_32()
				result_read_remainder = data_stream_peer.get_32()
				if result_read_remainder > 0:
					state = STATE_READBACK
				else:
					_finish_metadata()
					return true
		elif spt.get_status() == StreamPeerTCP.STATUS_ERROR:
			_internal_error("client: connection error - run caosprox.exe?")
			return true
	if state == STATE_READBACK:
		if spt.get_available_bytes() >= 0:
			var res = spt.get_data(result_read_remainder)
			var res_err = res[0]
			var res_data: PoolByteArray = res[1]
			if res_err != OK:
				_internal_error("client: interruption during data read")
				return true
			else:
				result.append_array(res_data)
				result_read_remainder -= len(res_data)
				if result_read_remainder == 0:
					_finish_metadata()
					return true
		elif spt.get_status() == StreamPeerTCP.STATUS_ERROR:
			_internal_error("client: connection error during readback")
			return true
	return false
