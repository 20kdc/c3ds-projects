#!/usr/bin/env python3
# Test program to dump some brain state to see how the format compares between CPXonCIE and CPX-W32

# caosprox - CPX server reference implementation
# Written starting in 2022 by contributors (see CREDITS.txt)
# To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
# You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

import libkc3ds.cpx as libcpx

f = open("lobe0.bin", "wb")
f.write(libcpx.raw_request_default(b"execute\ntarg norn\nbrn: dmpl 0\0"))
f.close()

