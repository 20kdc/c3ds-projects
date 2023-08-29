#!/usr/bin/env python3
# Used in chichi.py and such.

# caosprox - CPX server reference implementation
# Written starting in 2022 by contributors (see CREDITS.txt)
# To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
# You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

import os

class GizmoNode():
	"""
	Node in Gizmo tree
	"""
	def __init__(self):
		self.props = {}

	def gen_frames(self, frames, props):
		if len(self.props) != 0:
			props = props.copy()
			for key in self.props:
				props[key] = self.props[key]
		self.gen_frames_impl(frames, props)

	def gen_frames_impl(self, frames, props):
		raise Exception("Oh, if I were but not a pure virtual, what wonders would I show thee?")

	def p(self, prop, val):
		self.props[prop] = val

class CAIDAllocator():
	"""
	Allocates CAxxxx.bmp filenames
	"""
	def __init__(self, base):
		self.base = base
		self.idx = 0

	def allocate(self):
		res = os.path.join(self.base, "CA%04d.bmp" % (self.idx))
		self.idx += 1
		return res

class CSet(GizmoNode):
	"""
	A Set covers 56 sprite files for a C3/DS Norn.
	The other 56 (for the other sex) are simply copies. (Checked against ChiChi template)
	"""
	def __init__(self, ages):
		super().__init__()
		self.ages = ages

	def gen_frames_impl(self, frames, props):
		# establish M/F value proxies
		props_m = props.copy()
		props_m["male"] = 1
		props_m["female"] = 0
		props_m["filepath"] = os.path.join(props["filepath"], "male")
		props_f = props.copy()
		props_f["male"] = 0
		props_f["female"] = 1
		props_f["filepath"] = os.path.join(props["filepath"], "female")
		for age in self.ages:
			age.gen_frames(frames, props_m)
			age.gen_frames(frames, props_f)

class CAge(GizmoNode):
	def __init__(self, code, val, scale, parts):
		super().__init__()
		self.p("age", val)
		self.p("scale", scale)
		self.code = code
		self.val = val
		self.scale = scale
		self.parts = parts

	def gen_frames_impl(self, frames, props):
		props = props.copy()
		props["filepath"] = os.path.join(props["filepath"], self.code)
		props["caid"] = CAIDAllocator(props["filepath"])
		for part in self.parts:
			part.gen_frames(frames, props)

class CPart(GizmoNode):
	def __init__(self, pbi, size):
		super().__init__()
		self.pbi = pbi
		self.size = size

	def gen_frames_impl(self, frames, props):
		frame_idx = 1
		for frame in self.pbi.frames:
			for direction in self.pbi.directions:
				props = props.copy()
				props["real"] = True
				props["filepath"] = props["caid"].allocate()
				props["pitch"] = direction[0]
				props["yaw"] = direction[1]
				props["part_name"] = self.pbi.name
				props["frame"] = frame_idx
				props["frame_name"] = frame
				props["size"] = self.size
				frames.append(props)
			frame_idx += 1

class CPartInfo():
	"""
	Generalized information about a part not specific to one model size.
	"""
	def __init__(self, pid, name, frames, directions):
		super().__init__()
		self.pid = pid
		self.name = name
		self.frames = frames
		self.directions = directions

class CDummyPart(GizmoNode):
	"""
	A "part" that fills up the framesheet for the skeleton.
	Used for cases where we don't want unused stuff breaking UX
	"""
	def __init__(self, pid, count):
		super().__init__()
		self.pid = pid
		self.count = count

	def gen_frames_impl(self, frames, props):
		for i in range(self.count):
			frames.append({"real": False, "filepath": props["caid"].allocate()})

