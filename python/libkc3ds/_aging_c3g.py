#!/usr/bin/env python3
# Database of templates.

# libkc3ds - General-purpose Python library for C3/DS
# Written starting in 2023 by contributors (see CREDITS.txt)
# To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
# You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

from ._aging_defs import CSet, CAge, AgedPart
from libkc3ds.parts import C3, C3_a, C3_0, C3_b, C3_c, C3_d, C3_e, C3_f, C3_g, C3_h, C3_i, C3_j, C3_k, C3_l, C3_m, C3_n

C3_GRENDEL = CSet("C3_GRENDEL", "Jungle Grendel", C3, [
	# 0
	CAge("01", 0.6, [
		AgedPart(C3_a, 78),
		AgedPart(C3_0, 60),
		AgedPart(C3_c, 24),
		AgedPart(C3_d, 24),
		AgedPart(C3_e, 36),
		AgedPart(C3_f, 24),
		AgedPart(C3_g, 24),
		AgedPart(C3_h, 36),
		AgedPart(C3_i, 22),
		AgedPart(C3_j, 37),
		AgedPart(C3_k, 22),
		AgedPart(C3_l, 37),
		AgedPart(C3_b, 60),
		AgedPart(C3_m, 27),
		AgedPart(C3_n, 28),
	]),
	# 2
	CAge("23", 0.8, [
		AgedPart(C3_a, 104),
		AgedPart(C3_0, 80),
		AgedPart(C3_c, 32),
		AgedPart(C3_d, 32),
		AgedPart(C3_e, 48),
		AgedPart(C3_f, 32),
		AgedPart(C3_g, 32),
		AgedPart(C3_h, 48),
		AgedPart(C3_i, 30),
		AgedPart(C3_j, 49),
		AgedPart(C3_k, 30),
		AgedPart(C3_l, 49),
		AgedPart(C3_b, 80),
		AgedPart(C3_m, 36),
		AgedPart(C3_n, 37),
	]),
	# 4, 5
	CAge("456789", 1.0, [
		AgedPart(C3_a, 130),
		AgedPart(C3_0, 100),
		AgedPart(C3_c, 40),
		AgedPart(C3_d, 40),
		AgedPart(C3_e, 60),
		AgedPart(C3_f, 40),
		AgedPart(C3_g, 40),
		AgedPart(C3_h, 60),
		AgedPart(C3_i, 38),
		AgedPart(C3_j, 62),
		AgedPart(C3_k, 38),
		AgedPart(C3_l, 62),
		AgedPart(C3_b, 100),
		AgedPart(C3_m, 45),
		AgedPart(C3_n, 47),
	]),
])

