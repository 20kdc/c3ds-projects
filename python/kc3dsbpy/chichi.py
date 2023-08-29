#!/usr/bin/env python3
# Table of tables.

# caosprox - CPX server reference implementation
# Written starting in 2022 by contributors (see CREDITS.txt)
# To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
# You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

from .database import CSet, CAge, CPart
from .c3ds import P_a, D_0, P_b, P_c, P_d, P_e, P_f, P_g, P_h, P_i, P_j, P_k, P_l, P_m, P_n

CHICHI = CSet([
	CAge("0", 0, 0.6, [
		CPart(P_a, 60),
		D_0,
		CPart(P_c, 24),
		CPart(P_d, 24),
		CPart(P_e, 24),
		CPart(P_f, 24),
		CPart(P_g, 24),
		CPart(P_h, 24),
		CPart(P_i, 20),
		CPart(P_j, 24),
		CPart(P_k, 20),
		CPart(P_l, 24),
		CPart(P_b, 40),
		CPart(P_m, 18),
		CPart(P_n, 19)
	]),
	CAge("2", 2, 0.8, [
		CPart(P_a, 80),
		D_0,
		CPart(P_c, 32),
		CPart(P_d, 32),
		CPart(P_e, 32),
		CPart(P_f, 32),
		CPart(P_g, 32),
		CPart(P_h, 32),
		CPart(P_i, 27),
		CPart(P_j, 32),
		CPart(P_k, 27),
		CPart(P_l, 32),
		CPart(P_b, 54),
		CPart(P_m, 24),
		CPart(P_n, 25)
	]),
	CAge("4", 4, 1.0, [
		CPart(P_a, 100),
		D_0,
		CPart(P_c, 40),
		CPart(P_d, 40),
		CPart(P_e, 40),
		CPart(P_f, 40),
		CPart(P_g, 40),
		CPart(P_h, 40),
		CPart(P_i, 34),
		CPart(P_j, 40),
		CPart(P_k, 34),
		CPart(P_l, 40),
		CPart(P_b, 68),
		CPart(P_m, 30),
		CPart(P_n, 32)
	]),
	CAge("5", 5, 1.0, [
		CPart(P_a, 100),
		D_0,
		CPart(P_c, 40),
		CPart(P_d, 40),
		CPart(P_e, 40),
		CPart(P_f, 40),
		CPart(P_g, 40),
		CPart(P_h, 40),
		CPart(P_i, 34),
		CPart(P_j, 40),
		CPart(P_k, 34),
		CPart(P_l, 40),
		CPart(P_b, 68),
		CPart(P_m, 30),
		CPart(P_n, 32)
	])
])

