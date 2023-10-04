#!/usr/bin/env python3
# Central Part IDs list

# libkc3ds - General-purpose Python library for C3/DS
# Written starting in 2023 by contributors (see CREDITS.txt)
# To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
# You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

ALL = {}

class PartInfo():
	"""
	Information for how a part is identified in a game.
	Each frame is a string -> arbitrary dictionary containing details.
	Further details (self.part_id and self.frame_base) are filled out when the PartInfo is added to the Setup.
	Frames gain "frame", "part" and "blank" keys during this point as well.
	"""
	def __init__(self, char, att_point_names, frames, blank = False):
		self.char = char
		self.att_point_names = att_point_names
		self.frames = frames
		self.blank = blank
		self.part_id = None
		self.frame_base = None
	def setup_registers(self, part_id, base):
		"""
		Called during Setup constructor to attach this PartInfo
		"""
		self.part_id = part_id
		frame_idx = 0
		for f in self.frames:
			f["frame"] = frame_idx + base
			f["frame_rel"] = frame_idx
			f["part"] = self.part_id.name
			if self.blank:
				f["blank"] = 1
			else:
				f["blank"] = 0
			frame_idx += 1
		self.frame_base = base

class PartID():
	"""
	Part. self.name is unique and doesn't contain spaces etc.
	Creating a PartID auto-registers it with libkc3ds.parts.ALL!
	So don't create them...
	"""
	def __init__(self, name, c3 = None, c2 = None, cv = None, unused = False):
		self.name = name
		self.games = {}
		if not (c3 is None):
			self.games["c3"] = c3
		if not (c2 is None):
			self.games["c2"] = c2
		if not (cv is None):
			self.games["cv"] = cv
		self.unused = unused
		ALL[name] = self

# ---- ALL PARTS ----

def gen_c3_frames(details, base = {}):
	total = []
	for detail in details:
		for yaw in [1, -1, 0, 2]:
			for pitch in [-1, 0, 1, 2]:
				res = base.copy()
				for k in detail:
					res[k] = detail[k]
				# these IDs have a somewhat more mathematical relation to rotations
				# this should make maths easier
				res["pitch_id"] = pitch
				res["yaw_id"] = yaw
				total.append(res)
	return total

def gen_pad_frames(count):
	total = []
	for i in range(count):
		total.append({})
	return total

def gen_c2_frames(count):
	# until someone really needs this
	return gen_pad_frames(count * 10)

ATT_C2EHEAD = ["Body", "Mouth", "LeftEar (CV)", "RightEar (CV)", "Hair (CV)"]
ATT_C2EBODY = ["Head", "LeftThigh", "RightThigh", "LeftUpperArm", "RightUpperArm", "TailRoot"]
ATT_LIMB = ["Start", "End"]

C2_a = C3_a = CV_a = PartID("Head",
c3 = PartInfo("a", ATT_C2EHEAD, gen_c3_frames([
	{"expr": "normal", "eyes": 1, "normal": 1}, {"expr": "normal", "eyes": 0, "normal": 1},
	{"expr": "happy",  "eyes": 1, "happy": 1},  {"expr": "happy",  "eyes": 0, "happy": 1},
	{"expr": "sad",    "eyes": 1, "sad": 1},    {"expr": "sad",    "eyes": 0, "sad": 1},
	{"expr": "angry",  "eyes": 1, "angry": 1},  {"expr": "angry",  "eyes": 0, "angry": 1},
	{"expr": "scared", "eyes": 1, "scared": 1}, {"expr": "scared", "eyes": 0, "scared": 1},
	{"expr": "sleepy", "eyes": 1, "sleepy": 1}, {"expr": "sleepy", "eyes": 0, "sleepy": 1}
], {"normal": 0, "happy": 0, "sad": 0, "angry": 0, "scared": 0, "sleepy": 0})),
c2 = PartInfo("a", [], gen_c2_frames(12))
)

C2_b = C3_b = CV_b = PartID("Body",
c3 = PartInfo("b", ATT_C2EBODY, gen_c3_frames([
	{"egg": 0}, {"egg": 1}, {"egg": 2}, {"egg": 3}
])),
c2 = PartInfo("b", [], gen_c2_frames(1))
)

C2_c = C3_c = CV_c = PartID("LeftThigh",
c3 = PartInfo("c", ATT_LIMB, gen_c3_frames([{}])),
c2 = PartInfo("c", ATT_LIMB, gen_c2_frames(1))
)

C2_d = C3_d = CV_d = PartID("LeftShin",
c3 = PartInfo("d", ATT_LIMB, gen_c3_frames([{}])),
c2 = PartInfo("d", ATT_LIMB, gen_c2_frames(1))
)

C2_e = C3_e = CV_e = PartID("LeftFoot",
c3 = PartInfo("e", ATT_LIMB, gen_c3_frames([{}])),
c2 = PartInfo("e", ATT_LIMB, gen_c2_frames(1))
)

C2_f = C3_f = CV_f = PartID("RightThigh",
c3 = PartInfo("f", ATT_LIMB, gen_c3_frames([{}])),
c2 = PartInfo("f", ATT_LIMB, gen_c2_frames(1))
)

C2_g = C3_g = CV_g = PartID("RightShin",
c3 = PartInfo("g", ATT_LIMB, gen_c3_frames([{}])),
c2 = PartInfo("g", ATT_LIMB, gen_c2_frames(1))
)

C2_h = C3_h = CV_h = PartID("RightFoot",
c3 = PartInfo("h", ATT_LIMB, gen_c3_frames([{}])),
c2 = PartInfo("h", ATT_LIMB, gen_c2_frames(1))
)

C2_i = C3_i = CV_i = PartID("LeftUpperArm",
c3 = PartInfo("i", ATT_LIMB, gen_c3_frames([{}])),
c2 = PartInfo("i", ATT_LIMB, gen_c2_frames(1))
)

C2_j = C3_j = CV_j = PartID("LeftLowerArm",
c3 = PartInfo("j", ATT_LIMB, gen_c3_frames([{}])),
c2 = PartInfo("j", ATT_LIMB, gen_c2_frames(1))
)

C2_k = C3_k = CV_k = PartID("RightUpperArm",
c3 = PartInfo("k", ATT_LIMB, gen_c3_frames([{}])),
c2 = PartInfo("k", ATT_LIMB, gen_c2_frames(1))
)

C2_l = C3_l = CV_l = PartID("RightLowerArm",
c3 = PartInfo("l", ATT_LIMB, gen_c3_frames([{}])),
c2 = PartInfo("l", ATT_LIMB, gen_c2_frames(1))
)

C2_m = C3_m = CV_m = PartID("TailRoot",
c3 = PartInfo("m", ATT_LIMB, gen_c3_frames([{}])),
c2 = PartInfo("m", ATT_LIMB, gen_c2_frames(1))
)

C2_n = C3_n = CV_n = PartID("TailTip",
c3 = PartInfo("n", ATT_LIMB, gen_c3_frames([{}])),
c2 = PartInfo("n", ATT_LIMB, gen_c2_frames(1))
)

# Ok, so.
# Because this addon is focused on QuickNorn compatibility, it's not possible to handle CA stuff in the "C3 game entry".
# And this part ID needs to exist so it can have part IDs allocated to it.

C3_0 = PartID("C3Unused",
c3 = PartInfo("0", [], gen_pad_frames(96), blank = True),
unused = True
)

# ---- Setups ----

SETUP = {}

class Setup():
	"""
	Game skeleton setup.
	self.name is unique and doesn't contain spaces etc.
	self.part_ids is a list of PartIDs.
	self.part_infos is a list of PartInfos.
	self.part_names_to_infos is a dictionary mapping PartID.name to PartInfos.
	The constructor sets the frame bases up.
	"""
	def __init__(self, name, part_ids):
		self.name = name
		self.part_ids = part_ids
		self.part_infos = []
		self.part_names_to_infos = {}
		self.frames = []
		frame = 0
		for pi in part_ids:
			pif = pi.games[name]
			pif.setup_registers(pi, frame)
			frame += len(pif.frames)
			self.part_infos.append(pif)
			self.part_names_to_infos[pi.name] = pif
			self.frames += pif.frames
		SETUP[name] = self

# Changing this will break QuickNorn frame ranges.
C3 = Setup("c3", [C3_a, C3_0, C3_c, C3_d, C3_e, C3_f, C3_g, C3_h, C3_i, C3_j, C3_k, C3_l, C3_b, C3_m, C3_n])
# These frame orderings are just made up, which means you can do whatever you like
C2 = Setup("c2", [C2_a, C2_c, C2_d, C2_e, C2_f, C2_g, C2_h, C2_i, C2_j, C2_k, C2_l, C2_b, C2_m, C2_n])
CV = Setup("cv", [])

# ---- OTHER ----

C3_GS_MAP = {
	"maleNorn": "0",
	"maleGrendel": "1",
	"maleEttin": "2",
	"maleGeat": "3",
	"femaleNorn": "4",
	"femaleGrendel": "5",
	"femaleEttin": "6",
	"femaleGeat": "7"
}

