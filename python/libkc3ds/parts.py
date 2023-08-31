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
	def __init__(self, char, frames, blank = False):
		self.char = char
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
	def __init__(self, name, c3 = None):
		self.name = name
		self.games = {}
		if not (c3 is None):
			self.games["c3"] = c3
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

C3_a = PartID("Head",          c3 = PartInfo("a", gen_c3_frames([
	{"expr": "normal", "eyes": 1, "expr_normal": 1}, {"expr": "normal", "eyes": 0, "expr_normal": 1},
	{"expr": "happy",  "eyes": 1, "expr_happy": 1},  {"expr": "happy",  "eyes": 0, "expr_happy": 1},
	{"expr": "sad",    "eyes": 1, "expr_sad": 1},    {"expr": "sad",    "eyes": 0, "expr_sad": 1},
	{"expr": "angry",  "eyes": 1, "expr_angry": 1},  {"expr": "angry",  "eyes": 0, "expr_angry": 1},
	{"expr": "scared", "eyes": 1, "expr_scared": 1}, {"expr": "scared", "eyes": 0, "expr_scared": 1},
	{"expr": "sleepy", "eyes": 1, "expr_sleepy": 1}, {"expr": "sleepy", "eyes": 0, "expr_sleepy": 1}
], {"expr_normal": 0, "expr_happy": 0, "expr_sad": 0, "expr_angry": 0, "expr_scared": 0, "expr_sleepy": 0})))
C3_b = PartID("Body",          c3 = PartInfo("b", gen_c3_frames([
	{"egg": 0}, {"egg": 1}, {"egg": 2}, {"egg": 3}
])))
C3_c = PartID("LeftThigh",     c3 = PartInfo("c", gen_c3_frames([{}])))
C3_d = PartID("LeftShin",      c3 = PartInfo("d", gen_c3_frames([{}])))
C3_e = PartID("LeftFoot",      c3 = PartInfo("e", gen_c3_frames([{}])))
C3_f = PartID("RightThigh",    c3 = PartInfo("f", gen_c3_frames([{}])))
C3_g = PartID("RightShin",     c3 = PartInfo("g", gen_c3_frames([{}])))
C3_h = PartID("RightFoot",     c3 = PartInfo("h", gen_c3_frames([{}])))
C3_i = PartID("LeftUpperArm",  c3 = PartInfo("i", gen_c3_frames([{}])))
C3_j = PartID("LeftLowerArm",  c3 = PartInfo("j", gen_c3_frames([{}])))
C3_k = PartID("RightUpperArm", c3 = PartInfo("k", gen_c3_frames([{}])))
C3_l = PartID("RightLowerArm", c3 = PartInfo("l", gen_c3_frames([{}])))
C3_m = PartID("TailRoot",      c3 = PartInfo("m", gen_c3_frames([{}])))
C3_n = PartID("TailTip",       c3 = PartInfo("n", gen_c3_frames([{}])))
C3_0 = PartID("Mouth",         c3 = PartInfo("0", gen_c3_frames([
	{"expr": "normal", "expr_normal": 1},
	{"expr": "happy",  "expr_happy": 1},
	{"expr": "sad",    "expr_sad": 1},
	{"expr": "angry",  "expr_angry": 1},
	{"expr": "scared", "expr_scared": 1},
	{"expr": "sleepy", "expr_sleepy": 1}
], {"expr_normal": 0, "expr_happy": 0, "expr_sad": 0, "expr_angry": 0, "expr_scared": 0, "expr_sleepy": 0}), blank = True))

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

C3 = Setup("c3", [C3_a, C3_0, C3_c, C3_d, C3_e, C3_f, C3_g, C3_h, C3_i, C3_j, C3_k, C3_l, C3_b, C3_m, C3_n])

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

