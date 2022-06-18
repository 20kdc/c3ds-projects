#!/usr/bin/env python3
# Attempt to translate a CIE server to a CPX client

# caosprox - CPX server reference implementation
# Written starting in 2022 by 20kdc
# To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
# You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

import sys
import socket
from tools import libcpx # this is so silly but it keeps the file organization sane
import traceback

cie_host = "localhost"
cie_port = 20001
cpx_host = "localhost"
cpx_port = 19960

if len(sys.argv) == 5:
	cie_host = sys.argv[1]
	cie_port = int(sys.argv[2])
	cpx_host = sys.argv[3]
	cpx_port = int(sys.argv[4])
elif len(sys.argv) == 3:
	cie_host = sys.argv[1]
	cie_port = int(sys.argv[2])
elif len(sys.argv) == 1:
	# all defaults!
	pass
else:
	raise Exception("expects: [CIE_HOST CIE_PORT [CPX_HOST CPX_PORT]]")

# --- CIE Client ---

# returns data
def send_cie_request(req: bytes):
	cie_client = socket.create_connection((cie_host, cie_port))
	try:
		cie_client.sendall(req)
		# now receive data
		data = b""
		while True:
			chunk = cie_client.recv(1024)
			if chunk == b"":
				break
			data += chunk
		return data
	finally:
		cie_client.close()

# --- CIE-CPX Translator ---

# returns tuple (result_code, data)
def send_cpx_execute_to_cie(req: bytes):
	# uh just pass it as-is for now?????
	# as for the result, the null terminator is added regardless, which may or may not be a good idea
	return 0, send_cie_request(req + b"\nrscr") + b"\0"

# returns tuple (result_code, data)
def send_cpx_request_to_cie(req: bytes):
	req_all = libcpx.cut_terminated(req, b"\0")[0]
	req_ab = libcpx.cut_terminated(req_all, b"\n")
	cmd = req_ab[0]
	if cmd == b"execute":
		return send_cpx_execute_to_cie(req_ab[1])
	elif cmd == b"iscr":
		return send_cpx_execute_to_cie(req_ab[1])
	elif cmd.startswith(b"scrp "):
		# this isn't really a foolproof way to translate it but it's close enough
		return send_cpx_execute_to_cie(req_all + b"\nendm")
	return 1, b"caosproxy: unknown command @ CIE-CPX translator\0"

# --- CPX Server ---

initial_header = libcpx.CSMIHead().to_bytes()

def send_cpx_response(conn: socket.socket, code: int, data: bytes):
	hdr = libcpx.CSMIHead()
	hdr.result_code = code
	hdr.data_len = len(data)
	conn.sendall(hdr.to_bytes() + data)

def internal_error(conn: socket.socket, stage: str, error: Exception):
	print("Error during " + stage + ": ")
	traceback.print_exception(type(error), error, error.__traceback__)
	send_cpx_response(conn, 1, ("caosproxy: " + stage).encode("latin1") + b"\x00")

def handle_connection(conn: socket.socket):
	# send initial header. if we don't even manage this, just bail out on the connection entirely.
	try:
		conn.sendall(initial_header)
	except Exception as error:
		print("Error too early to send!")
		traceback.print_exception(type(error), error, error.__traceback__)
		return
	try:
		reqlen = libcpx.decode_cpxrhead(libcpx.recvall(conn, 4))
	except Exception as error:
		internal_error(conn, "receiving request length: " + str(error), error)
		return
	try:
		reqdata = libcpx.recvall(conn, reqlen)
	except Exception as error:
		internal_error(conn, "receiving request data: " + str(error), error)
		return
	try:
		res_code, res_data = send_cpx_request_to_cie(reqdata)
	except Exception as error:
		internal_error(conn, "request meta error, check console: " + str(error), error)
		return
	try:
		send_cpx_response(conn, res_code, res_data)
	except Exception as error:
		# we were busy sending a response, what are we going to do?
		print("Error too late to send!")
		traceback.print_exception(type(error), error, error.__traceback__)

cpx_hostport = (cpx_host, cpx_port)
serversocket = socket.create_server(cpx_hostport)

print("Awaiting connections on " + str(cpx_hostport))

while True:
	conn = serversocket.accept()[0]
	handle_connection(conn)
	conn.close()

