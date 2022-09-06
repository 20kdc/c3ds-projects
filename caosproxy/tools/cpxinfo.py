#!/usr/bin/env python3
# CAOS Terminal

# caosprox - CPX server reference implementation
# Written starting in 2022 by contributors (see CREDITS.txt)
# To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
# You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

import traceback
import libcpx

def rrdo(f: str, t: bytes) -> str:
	try:
		print("\t" + f + ":", libcpx.raw_request_default(t)[:-1].decode(libcpx.enc))
	except Exception as error:
		print("During:", f)
		traceback.print_exception(type(error), error, error.__traceback__)

# ---
print("SMI Header:")
srl = libcpx.open_default()
cpxr = libcpx.CSMIHead(libcpx.recvall(srl, libcpx.csmihead_len))
print("\tengine ID:", cpxr.magic.decode("latin1"))
print("\tpayload capacity:", cpxr.data_len_max)
srl.close()
# ---
print("Game information:")
rrdo("game name", b"execute\nouts gnam\0")
rrdo("engine version", b"execute\noutv vmjr outs \".\" outv vmnr\0")
rrdo("engine modules", b"execute\nouts modu\0")
# ---
print("CPX extensions:")
rrdo("server", b"cpx-ver\n\0")
rrdo("game path", b"cpx-gamepath\n\0")
# ---

