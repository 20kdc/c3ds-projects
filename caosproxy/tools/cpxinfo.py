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

def rrdo(t: bytes) -> str:
	return libcpx.raw_request_default(t)[:-1].decode("latin1")

print("server:", rrdo(b"cpx-ver\n\0"))
print("game path:", rrdo(b"cpx-gamepath\n\0"))
print("game name:", rrdo(b"execute\nouts gnam\0"))
print("engine version:", rrdo(b"execute\noutv vmjr outs \".\" outv vmnr\0"))
print("engine modules:", rrdo(b"execute\nouts modu\0"))

