class_name CPXRequest
extends Reference

const STATE_INIT = 0
const STATE_HEADER1 = 1
const STATE_HEADER2 = 2
const STATE_READBACK = 3
const STATE_FINISHED = 4

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
	var rdx = StreamPeerBuffer.new()
	rdx.big_endian = false
	rdx.put_32(len(r))
	rdx.put_data(r)
	request = rdx.data_array

func to_string() -> String:
	var state_str = "unknown state"
	if state == STATE_INIT:
		state_str = "INIT"
	elif state == STATE_HEADER1:
		state_str = "HEADER1 (" + str(spt.get_available_bytes()) + " AVB)"
	elif state == STATE_HEADER2:
		state_str = "HEADER2 (" + str(spt.get_available_bytes()) + " AVB)"
	elif state == STATE_READBACK:
		state_str = "READBACK (" + str(len(result)) + "/" + str(result_read_remainder) + ")"
	elif state == STATE_FINISHED:
		state_str = "FINISHED (code: " + str(result_code) + ", internal: " + str(result_error_internal) + ")"
	return purpose + ": " + state_str

func result_str() -> String:
	var tmp: PoolByteArray = result
	if len(tmp) > 0:
		if tmp[len(tmp) - 1] == 0:
			tmp.remove(len(tmp) - 1)
	return tmp.get_string_from_ascii()

func terminate(text: String):
	if state == STATE_FINISHED:
		return
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
	if spt != null:
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
		# NOTE: Don't make this "localhost", it doesn't work on Windows
		if spt.connect_to_host("127.0.0.1", 19960) != OK:
			terminate("client: failed to open connection - run caosprox.exe!")
			return true
		spt.set_no_delay(true)
		if spt.put_data(request) != OK:
			terminate("client: failed to write request - run caosprox.exe!")
			return true
		state = STATE_HEADER1
	if state == STATE_HEADER1:
		if spt.get_available_bytes() >= 24:
			var res = spt.get_data(24)
			var res_err = res[0]
			if res_err != OK:
				terminate("client: could not get headers")
				return true
			state = STATE_HEADER2
		elif spt.get_status() == StreamPeerTCP.STATUS_ERROR:
			terminate("client: connection error - run caosprox.exe?")
			return true
	if state == STATE_HEADER2:
		if spt.get_available_bytes() >= 24:
			var res = spt.get_data(24)
			var res_err = res[0]
			var res_data: PoolByteArray = res[1]
			if res_err != OK:
				terminate("client: could not get headers")
				return true
			else:
				var data_stream_peer = StreamPeerBuffer.new()
				data_stream_peer.data_array = res_data
				data_stream_peer.big_endian = false
				data_stream_peer.seek(8)
				result_code = data_stream_peer.get_32()
				result_read_remainder = data_stream_peer.get_32()
				if result_read_remainder > 0:
					state = STATE_READBACK
				else:
					_finish_metadata()
					return true
		elif spt.get_status() == StreamPeerTCP.STATUS_ERROR:
			terminate("client: connection error - run caosprox.exe?")
			return true
	if state == STATE_READBACK:
		if spt.get_available_bytes() >= 0:
			var res = spt.get_data(result_read_remainder)
			var res_err = res[0]
			var res_data: PoolByteArray = res[1]
			if res_err != OK:
				terminate("client: interruption during data read")
				return true
			else:
				result.append_array(res_data)
				result_read_remainder -= len(res_data)
				if result_read_remainder == 0:
					_finish_metadata()
					return true
		elif spt.get_status() == StreamPeerTCP.STATUS_ERROR:
			terminate("client: connection error during readback")
			return true
	return false
