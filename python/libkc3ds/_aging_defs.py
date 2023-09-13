#!/usr/bin/env python3
# Database of templates.

# libkc3ds - General-purpose Python library for C3/DS
# Written starting in 2023 by contributors (see CREDITS.txt)
# To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
# You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

from libkc3ds.parts import C3, C3_a, C3_0, C3_b, C3_c, C3_d, C3_e, C3_f, C3_g, C3_h, C3_i, C3_j, C3_k, C3_l, C3_m, C3_n

class CSet():
	"""
	A Set covers 56 sprite files for a C3/DS Norn.
	The other 56 (for the other sex) are simply copies. (Checked against ChiChi template)
	self.ages maps age chars to CAge.
	"""
	def __init__(self, name, desc, setup, ages):
		self.name = name
		self.desc = desc
		self.setup = setup
		self.ages = {}
		for age in ages:
			for char in age.chars:
				self.ages[char] = age

class CAge():
	"""
	self.parts maps part names to AgedPart.
	"""
	def __init__(self, chars, scale, parts):
		self.chars = chars
		self.scale = scale
		self.parts = {}
		for part in parts:
			self.parts[part.part_id.name] = part

class AgedPart():
	def __init__(self, part_id, size):
		self.part_id = part_id
		self.size = size

