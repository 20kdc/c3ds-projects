#!/usr/bin/env python3

# libkc3ds - General-purpose Python library for C3/DS
# Written starting in 2023 by contributors (see CREDITS.txt)
# To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
# You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

# Just exists to figure out some stuff don't mind me
syllable_types = ["V", "CV", "VN", "CVN"]
for i in syllable_types:
	for j in syllable_types:
		for k in syllable_types:
			total = i + j + k
			if len(total) != 6:
				continue
			if "VV" in total:
				continue
			print(total)
