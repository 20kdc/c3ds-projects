#!/usr/bin/env python3
# Used in chichi.py and such.

# caosprox - CPX server reference implementation
# Written starting in 2022 by contributors (see CREDITS.txt)
# To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
# You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

from .database import CPartInfo, CDummyPart

# C3DS part rotations

TILT_1 = 22.5
TILT_2 = 0
TILT_3 = -22.5
TILT_4 = -45

DIRECTIONS = [
	[TILT_1,  90],
	[TILT_2,  90],
	[TILT_3,  90],
	[TILT_4,  90],
	[TILT_1, -90],
	[TILT_2, -90],
	[TILT_3, -90],
	[TILT_4, -90],
	[TILT_1,   0],
	[TILT_2,   0],
	[TILT_3,   0],
	[TILT_4,   0],
	[TILT_1, 180],
	[TILT_2, 180],
	[TILT_3, 180],
	[TILT_4, 180]
]

# C3DS part infos

P_a = CPartInfo("a", "Head", ["Normal", "Normal-Eyes-Closed", "Happy", "Happy-Eyes-Closed", "Sad", "Sad-Eyes-Closed", "Angry", "Angry-Eyes-Closed", "Scared", "Scared-Eyes-Closed", "Sleepy", "Sleepy-Eyes-Closed"], DIRECTIONS)
P_b = CPartInfo("b", "Body", ["Stage1", "Stage2", "Stage3", "Stage4"], DIRECTIONS)
P_c = CPartInfo("c", "LeftThigh", ["Normal"], DIRECTIONS)
P_d = CPartInfo("d", "LeftShin", ["Normal"], DIRECTIONS)
P_e = CPartInfo("e", "LeftFoot", ["Normal"], DIRECTIONS)
P_f = CPartInfo("f", "RightThigh", ["Normal"], DIRECTIONS)
P_g = CPartInfo("g", "RightShin", ["Normal"], DIRECTIONS)
P_h = CPartInfo("h", "RightFoot", ["Normal"], DIRECTIONS)
P_i = CPartInfo("i", "LeftUpperArm", ["Normal"], DIRECTIONS)
P_j = CPartInfo("j", "LeftLowerArm", ["Normal"], DIRECTIONS)
P_k = CPartInfo("k", "RightUpperArm", ["Normal"], DIRECTIONS)
P_l = CPartInfo("l", "RightLowerArm", ["Normal"], DIRECTIONS)
P_m = CPartInfo("m", "TailRoot", ["Normal"], DIRECTIONS)
P_n = CPartInfo("n", "TailTip", ["Normal"], DIRECTIONS)

D_0 = CDummyPart("0", 96)

