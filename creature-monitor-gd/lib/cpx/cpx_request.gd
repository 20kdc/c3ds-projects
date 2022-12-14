class_name CPXRequest
extends Reference

const STATE_INIT = 0
const STATE_CONNECTING = 1
const STATE_HEADER1 = 2
const STATE_HEADER2 = 3
const STATE_READBACK = 4
const STATE_FINISHED = 5

var purpose: String
var conn: CPXConnector
var request: PoolByteArray
var result: PoolByteArray
var result_code: int = 0
var result_error_internal: bool = false
var result_read_remainder: int = 0
var state: int = STATE_INIT
var last_status: String = ""

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
	elif state == STATE_CONNECTING:
		state_str = "CONNECTING (" + conn.get_status() + ")"
	elif state == STATE_HEADER1:
		state_str = "HEADER1 (" + conn.get_status() + ")"
	elif state == STATE_HEADER2:
		state_str = "HEADER2 (" + conn.get_status() + ")"
	elif state == STATE_READBACK:
		state_str = "READBACK (" + conn.get_status() + ")"
	elif state == STATE_FINISHED:
		state_str = "FINISHED (" + last_status + ", code: " + str(result_code) + ", internal: " + str(result_error_internal) + ")"
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
	if conn != null:
		last_status = conn.get_status()
		conn.close()
		conn = null
	emit_signal("completed")

# True == done!
func poll() -> bool:
	# Deliberate fallthrough
	if state == STATE_FINISHED:
		return true
	if state == STATE_INIT:
		# Named pipe method, disabled for now because unstable
		#if OS.get_name() == "Windows":
		#	var f = File.new()
		#	if f.open("\\\\.\\pipe\\CAOSWorkaroundBecauseWindowsIsAFuckedUpPieceOfShit", File.READ_WRITE) == OK:
		#		conn = CPXConnectorFile.new(f)
		if conn == null:
			# TCP method
			var spt = StreamPeerTCP.new()
			# NOTE: Don't make this "localhost", it doesn't work on Windows
			if spt.connect_to_host("127.0.0.1", 19960) == OK:
				conn = CPXConnectorTCP.new(spt)
		if conn == null:
			terminate("client: failed to open connection - run caosprox.exe!")
			return true
		state = STATE_CONNECTING
	if state == STATE_CONNECTING:
		if conn.is_broken():
			terminate("client: failed to connect - run caosprox.exe!")
		elif not conn.is_connecting():
			if conn.write(request) != OK:
				terminate("client: failed to write request - run caosprox.exe!")
				return true
			state = STATE_HEADER1
	if state == STATE_HEADER1:
		if conn.expect(24) and conn.read(24):
			state = STATE_HEADER2
		elif conn.is_broken():
			terminate("client: connection error - run caosprox.exe?")
			return true
	if state == STATE_HEADER2:
		if conn.expect(24) and conn.read(24):
			var data_stream_peer = StreamPeerBuffer.new()
			data_stream_peer.data_array = conn.last_read
			data_stream_peer.big_endian = false
			data_stream_peer.seek(8)
			result_code = data_stream_peer.get_32()
			result_read_remainder = data_stream_peer.get_32()
			if result_read_remainder > 0:
				state = STATE_READBACK
			else:
				_finish_metadata()
				return true
		elif conn.is_broken():
			terminate("client: connection error - run caosprox.exe?")
			return true
	if state == STATE_READBACK:
		if conn.read(result_read_remainder):
			result = conn.last_read
			_finish_metadata()
			return true
		elif conn.is_broken():
			terminate("client: interruption during data read")
			return true
	return false
