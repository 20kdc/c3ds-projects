#!/usr/bin/env python3
# Injects a whole file.

# caosprox - CPX server reference implementation
# Written starting in 2022 by contributors (see CREDITS.txt)
# To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
# You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

import libcpx
import sys

f = open(sys.argv[1], "r")
text = f.read()
f.close()

print(libcpx.execute_caos_default(text))

