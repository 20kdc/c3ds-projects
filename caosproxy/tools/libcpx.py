#!/usr/bin/env python3
# CAOS Proxy interface library for Python 3
# To understand this, you should read:
#  caosproxy/spec.txt
#  http://double.nz/creatures/developer/sharedmemory.htm
# When copying, it is recommended you write down the commit hash here:
# ________________________________________

# caosprox - CPX server reference implementation
# Written starting in 2022 by contributors (see CREDITS.txt)
# To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
# You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

import socket
import os
import struct

enc = "windows-1252"

C2E_MAGIC = b"c2e@"

class CSMIHead():
	"""
	Class to manage a shared memory interface header.
	"""
	def __init__(self, b: bytes = None):
		"""
		Initializes a blank default shared memory interface header object.
		If bytes are supplied, loads the contents of said bytes into this object via of_bytes.
		"""
		self.magic = C2E_MAGIC
		self.process_id = 0
		self.result_code = 0
		self.data_len = 0
		self.data_len_max = 1048576
		self.padding = 0
		if not (b is None):
			self.of_bytes(b)
	def is_valid(self) -> bool:
		"""
		Returns True if this header is valid.
		"""
		return self.magic == C2E_MAGIC
	def of_bytes(self, b: bytes):
		"""
		Loads the contents of the given bytes into the fields of this object.
		"""
		unpacked = struct.unpack("<4sIiIIi", b)
		self.magic = unpacked[0]
		self.process_id = unpacked[1]
		self.result_code = unpacked[2]
		self.data_len = unpacked[3]
		self.data_len_max = unpacked[4]
		self.padding = unpacked[5]
	def to_bytes(self) -> bytes:
		"""
		Converts this object back to a shared memory interface header's bytes.
		"""
		return struct.pack("<4sIiIIi", self.magic, self.process_id, self.result_code, self.data_len, self.data_len_max, self.padding)
	def __repr__(self):
		"""
		Provides a description of the object fields.
		"""
		return "CSMIHead: magic=" + self.magic.decode(enc) + " pid=" + str(self.process_id) + " code=" + str(self.result_code) + " dl=" + str(self.data_len) + " dlm=" + str(self.data_len_max) + " pad=" + str(self.padding)

def recvall(s: socket.socket, l: int) -> bytes:
	"""
	Calls recv repeatedly until end of stream (raising EOFError if encountered early) or until the given length has been fulfilled.
	"""
	data = b""
	while len(data) < l:
		chunk = s.recv(l - len(data))
		if chunk == b"":
			raise EOFError("at " + str(len(data)) + " during recvall(" + str(s) + ", " + str(l) + ")")
		data += chunk
	return data

csmihead_len = 24

def decode_cpxrhead(b: bytes) -> int:
	"""
	Decodes a CPX request header (just an integer for the length)
	"""
	return struct.unpack("<I", b)[0]
def encode_cpxr(b: bytes) -> bytes:
	"""
	Encodes a CPX request (i.e. adds a length prefix).
	"""
	return struct.pack("<I", len(b)) + b
cpxrhead_len = 4

def cut_terminated(b: bytes, t: bytes):
	idx = b.find(t)
	if idx == -1:
		return b, b""
	return b[:idx], b[idx + len(t):]

def open_default() -> socket:
	"""
	Opens a socket to the "default" CPX server.
	"""
	return socket.create_connection((os.getenv("CPX_HOST") or "localhost", os.getenv("CPX_PORT") or 19960))

class CPXError(Exception):
	"""
	This exception type is specifically for errors returned by the remote host.
	Any networking errors or such are returned as other exceptions.
	"""
	pass

# -- main client functions --

def raw_request(s: socket.socket, request: bytes) -> bytes:
	"""
	Performs a raw CPX request, sending and returning binary data.
	Realistically, what you're sending is always going to be a *null-terminated* string.
	The main purpose of this function is that it handles the fiddly bits (i.e. raising CPXError).
	"""
	hdr1 = CSMIHead(recvall(s, csmihead_len))
	if not hdr1.is_valid():
		raise Exception("Server description header not valid")
	# send request
	s.sendall(encode_cpxr(request))
	hdr2 = CSMIHead(recvall(s, csmihead_len))
	resp = recvall(s, hdr2.data_len)
	if not hdr2.is_valid():
		raise Exception("Response header not valid")
	if hdr2.result_code != 0:
		# Error
		raise CPXError(cut_terminated(resp, b"\0")[0].decode(enc))
	return resp

def execute_caos(s: socket.socket, t: str) -> str:
	"""
	Runs some CAOS and gets a textual result.
	This isn't qualified to handle binary data as it expects (and removes) the null terminator and decodes the string.
	"""
	resp = raw_request(s, b"execute\n" + (t.encode(enc)) + b"\0")
	return cut_terminated(resp, b"\0")[0].decode(enc)

# -- main client functions, defaults --

def raw_request_default(t: bytes) -> bytes:
	"""
	Performs a raw CPX request, sending and returning binary data.
	Realistically, what you're sending is always going to be a *null-terminated* string.
	The main purpose of this function is that it handles the fiddly bits (i.e. raising CPXError).
	"""
	s = open_default()
	try:
		return raw_request(s, t)
	finally:
		s.close()

def execute_caos_default(t: str) -> str:
	"""
	Runs some CAOS and gets a textual result.
	This isn't qualified to handle binary data as it expects (and removes) the null terminator and decodes the string.
	"""
	s = open_default()
	try:
		return execute_caos(s, t)
	finally:
		s.close()

