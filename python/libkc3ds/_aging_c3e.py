#!/usr/bin/env python3
# Database of templates.

# libkc3ds - General-purpose Python library for C3/DS
# Written starting in 2023 by contributors (see CREDITS.txt)
# To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
# You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

from ._aging_defs import CSet, CAge, AgedPart
from libkc3ds.parts import C3, C3_a, C3_b, C3_c, C3_d, C3_e, C3_f, C3_g, C3_h, C3_i, C3_j, C3_k, C3_l, C3_m, C3_n

C3_ETTIN = CSet("C3_ETTIN", "Desert Ettin", C3, [
	# 0
	CAge("01", 0.75, [
		AgedPart(C3_a, 60),
		AgedPart(C3_c, 24),
		AgedPart(C3_d, 24),
		AgedPart(C3_e, 24),
		AgedPart(C3_f, 24),
		AgedPart(C3_g, 24),
		AgedPart(C3_h, 24),
		AgedPart(C3_i, 20),
		AgedPart(C3_j, 24),
		AgedPart(C3_k, 20),
		AgedPart(C3_l, 24),
		AgedPart(C3_b, 40),
		AgedPart(C3_m, 18),
		AgedPart(C3_n, 19),
	]),
	# 2
	CAge("23", 0.875, [
		AgedPart(C3_a, 70),
		AgedPart(C3_c, 28),
		AgedPart(C3_d, 28),
		AgedPart(C3_e, 28),
		AgedPart(C3_f, 28),
		AgedPart(C3_g, 28),
		AgedPart(C3_h, 28),
		AgedPart(C3_i, 23),
		AgedPart(C3_j, 28),
		AgedPart(C3_k, 23),
		AgedPart(C3_l, 28),
		AgedPart(C3_b, 47),
		AgedPart(C3_m, 21),
		AgedPart(C3_n, 22) # Estimated. Desert Ettins don't have this part version.
	]),
	# 4, 5
	CAge("456789", 1.0, [
		AgedPart(C3_a, 80),
		AgedPart(C3_c, 32),
		AgedPart(C3_d, 32),
		AgedPart(C3_e, 32),
		AgedPart(C3_f, 32),
		AgedPart(C3_g, 32),
		AgedPart(C3_h, 32),
		AgedPart(C3_i, 27),
		AgedPart(C3_j, 32),
		AgedPart(C3_k, 27),
		AgedPart(C3_l, 32),
		AgedPart(C3_b, 54),
		AgedPart(C3_m, 24),
		AgedPart(C3_n, 25) # Estimated. Desert Ettins don't have this part version.
	]),
])

