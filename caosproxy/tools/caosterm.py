#!/usr/bin/env python3
# CAOS Terminal

# caosprox - CPX server reference implementation
# Written starting in 2022 by 20kdc
# To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
# You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

import socket
import readline
import sys
import struct

the_host = sys.argv[1]
the_port = int(sys.argv[2])

while True:
	text = input("caos> ")
	s = socket.socket()
	s.connect((the_host, the_port))
	# receive initial burst
	hdr1 = b""
	for i in range(24):
		hdr1 += s.recv(1)
	print("HDR1:", hdr1.hex())
	# send request
	req = b"execute\n" + text.encode("cp437") + b"\0"
	s.sendall(struct.pack("<I", len(req)))
	s.sendall(req)
	# receive secondary burst
	hdr2 = b""
	for i in range(24):
		hdr2 += s.recv(1)
	print("HDR2:", hdr2.hex())
	print("     ", "mmmmmmmmpppppppprrrrrrrrsssssssszzzzzzzz________")
	# receive response data
	resp_size = struct.unpack("<I", hdr2[12:16])[0]
	resp = b""
	for i in range(resp_size):
		resp += s.recv(1)
	print("RESP:", resp.decode("cp437"))
	s.close()

