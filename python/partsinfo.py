#!/usr/bin/env python3
# Check parts information and stuff

# libkc3ds - General-purpose Python library for C3/DS
# Written starting in 2023 by contributors (see CREDITS.txt)
# To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
# You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

import argparse
from libkc3ds.parts import SETUP

# Command definition

parser = argparse.ArgumentParser(prog="partsinfo.py", description="Shows information about parts.")
args = parser.parse_args()

for setup in SETUP:
	res = SETUP[setup]
	total = 0
	for pi in res.part_infos:
		total += len(pi.frames)
	print(setup + ", " + str(total) + " frames:")
	for pi in res.part_infos:
		if pi.blank:
			print("\t" + pi.part_id.name + ": '" + pi.char + "', BLANK, " + str(len(pi.frames)) + " frames:")
		else:
			print("\t" + pi.part_id.name + ": '" + pi.char + "', frames:")
		frame_idx = pi.frame_base
		for f in pi.frames:
			print("\t\t" + str(f))
			frame_idx += 1

