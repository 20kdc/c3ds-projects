#!/usr/bin/env python3

# libkc3ds - General-purpose Python library for C3/DS
# Written starting in 2023 by contributors (see CREDITS.txt)
# To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
# You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

# Just exists to figure out some stuff don't mind me
syllable_types = ["", "V", "CV", "VN", "CVN"]

done = {}

for i in syllable_types:
	for j in syllable_types:
		for k in syllable_types:
			total = i + j + k
			# must be 6 characters
			if len(total) != 6:
				continue
			# vowel clusters not allowed
			if "VV" in total:
				continue
			# to prevent a massive proliferation of 'N', substitute 'C' where valid
			total = total.replace("NV", "CV")
			# can't repeat
			if total in done:
				continue
			print(total)
			done[total] = True
