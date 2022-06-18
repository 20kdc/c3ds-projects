#!/usr/bin/env python3
# CAOS Proxy interface library for Python 3
# When copying, it is recommended you write down the commit hash here:
# ________________________________________

# caosprox - CPX server reference implementation
# Written starting in 2022 by 20kdc
# To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
# You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

import socket
import sys
import struct

class CSMIHead():
	def __init__(self, b: bytes = None):
		self.magic = b"c2e@"
		self.process_id = 0
		self.result_code = 0
		self.data_len = 0
		self.data_len_max = 1048576
		self.padding = 0
		if not (b is None):
			self.of_bytes(b)
	def is_valid(self):
		return self.magic == b"c2e@"
	def of_bytes(self, b: bytes):
		unpacked = struct.unpack("<4sIiIIi", b)
		self.magic = unpacked[0]
		self.process_id = unpacked[1]
		self.result_code = unpacked[2]
		self.data_len = unpacked[3]
		self.data_len_max = unpacked[4]
		self.padding = unpacked[5]
	def to_bytes(self):
		return struct.pack("<4sIiIIi", self.magic, self.process_id, self.result_code, self.data_len, self.data_len_max, self.padding) + self.data
	def __repr__(self):
		return "CSMIHead: magic=" + self.magic.decode("latin1") + " pid=" + str(self.process_id) + " code=" + str(self.result_code) + " dl=" + str(self.data_len) + " dlm=" + str(self.data_len_max) + " pad=" + str(self.padding)

def recvall(s: socket.socket, l: int):
	data = b""
	while len(data) < l:
		chunk = s.recv(l - len(data))
		if chunk == b"":
			raise EOFError("at " + str(len(data)) + " during recvall(" + str(s) + ", " + str(l) + ")")
		data += chunk
	return data

csmihead_len = 24

