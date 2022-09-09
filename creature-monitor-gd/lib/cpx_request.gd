class_name CPXRequest
extends Reference

const STATE_CONNECTING = 0
const STATE_READBACK = 1
const STATE_FINISHED = 2

var spt: StreamPeerTCP
var result: PoolByteArray
var result_code: int = 0
var result_error_internal: bool = false
var result_read_remainder: int = 0
var state: int = STATE_CONNECTING

func _init(request: PoolByteArray):
	spt = StreamPeerTCP.new()
	spt.big_endian = false
	# NOTE: Don't make this "localhost", it doesn't work on Windows
	if spt.connect_to_host("127.0.0.1", 19960) != OK:
		_internal_error("client: failed to open connection - run caosprox.exe!")
	spt.put_32(len(request))
	if spt.put_data(request) != OK:
		_internal_error("client: failed to write request - run caosprox.exe!")

static func from_caos(text: String) -> PoolByteArray:
	var pba = ("execute\n" + text).to_utf8()
	pba.push_back(0)
	return pba

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
	state = STATE_FINISHED

func _finish_closeoff():
	state = STATE_FINISHED
	if spt.is_connected_to_host():
		spt.disconnect_from_host()
	spt = null

# True == done!
func poll() -> bool:
	if state == STATE_FINISHED:
		return true
	elif state == STATE_CONNECTING:
		if spt.get_available_bytes() >= 48:
			var res = spt.get_data(48)
			var res_err = res[0]
			var res_data: PoolByteArray = res[1]
			if res_err != OK:
				_internal_error("client: could not get headers")
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
		elif spt.get_status() == StreamPeerTCP.STATUS_ERROR:
			_internal_error("client: connection error - run caosprox.exe?")
	elif state == STATE_READBACK:
		if spt.get_available_bytes() >= 0:
			var res = spt.get_data(result_read_remainder)
			var res_err = res[0]
			var res_data: PoolByteArray = res[1]
			if res_err != OK:
				_internal_error("client: interruption during data read")
			else:
				result.append_array(res_data)
				result_read_remainder -= len(res_data)
				if result_read_remainder == 0:
					_finish_metadata()
		elif spt.get_status() == StreamPeerTCP.STATUS_ERROR:
			_internal_error("client: connection error during readback")
	return state == STATE_FINISHED
