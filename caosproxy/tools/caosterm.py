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
import traceback

import libcpx

while True:
	text = input("caos> ")
	try:
		print(libcpx.execute_caos_default(text))
	except Exception as error:
		traceback.print_exception(type(error), error, error.__traceback__)

