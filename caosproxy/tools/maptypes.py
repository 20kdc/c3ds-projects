#!/usr/bin/env python3
# Maps scripts in the Scriptorium.

# caosprox - CPX server reference implementation
# Written starting in 2022 by contributors (see CREDITS.txt)
# To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
# You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

import libcpx

print("F,G,S")

def map_species(fid: int, gid: int, sid: int):
	print(str(fid) + "," + str(gid) + "," + str(sid))

def map_genus(fid: int, gid: int):
	for v in libcpx.execute_caos_default("gids gnus " + str(fid) + " " + str(gid)).split(" "):
		if v != "":
			map_species(fid, gid, int(v))

def map_family(fid: int):
	for v in libcpx.execute_caos_default("gids fmly " + str(fid)).split(" "):
		if v != "":
			map_genus(fid, int(v))

for v in libcpx.execute_caos_default("gids root").split(" "):
	if v != "":
		map_family(int(v))

