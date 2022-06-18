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

import libcpx

the_host = "localhost"
the_port = 19960

if len(sys.argv) >= 2:
	the_host = sys.argv[1]
if len(sys.argv) >= 3:
	the_port = int(sys.argv[2])

while True:
	text = input("caos> ")
	s = socket.socket()
	s.connect((the_host, the_port))
	# receive initial burst
	hdr = libcpx.CSMIHead(libcpx.recvall(s, libcpx.csmihead_len))
	print("IHDR:", hdr)
	# send request
	s.sendall(libcpx.encode_cpxr(b"execute\n" + text.encode("latin1") + b"\0"))
	# receive secondary burst
	hdr.of_bytes(libcpx.recvall(s, libcpx.csmihead_len))
	print("RHDR:", hdr)
	# receive response data
	resp = libcpx.recvall(s, hdr.data_len)
	# we don't bother getting rid of the null terminator for text output
	# of course we don't KNOW it's text output anyway
	# does it matter?
	print(resp.decode("latin1"))
	s.close()

