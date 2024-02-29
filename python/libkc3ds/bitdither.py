#!/usr/bin/env python3
# Used by s16 to perform 565 dithering, but is actually generalized for any number of bits.
# When copying, it is recommended you write down the commit hash here:
# ________________________________________

# caosprox - CPX server reference implementation
# Written starting in 2022 by contributors (see CREDITS.txt)
# To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
# You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

import random

# ---- Bitcopy/number management ----

# Generates a bitcopy table.
def _gen_bitcopy(bits: int):
	tbl = []
	mask = (0xFF00 >> bits) & 0xFF
	for i in range(256):
		vb = i & mask
		v = 0
		# Make all 8 possible duplicates
		for j in range(8):
			v |= vb >> (bits * j)
		tbl.append(v)
	return tbl

# Bitcopy tables for varying amounts of bits.
BITCOPY_TABLES = [
	_gen_bitcopy(0), # maps to all 0s but that's alright
	_gen_bitcopy(1),
	_gen_bitcopy(2),
	_gen_bitcopy(3),
	_gen_bitcopy(4),
	_gen_bitcopy(5),
	_gen_bitcopy(6),
	_gen_bitcopy(7),
	_gen_bitcopy(8)
]

def dither_point_mapprob_list(points: list, gamma: bool):
	"""
	Given a sorted list of 0-255 ints, creates a list of 256 entries as follows:
	[first, prob, second]
	first and second are values from points.
	prob is the floating-point fraction to blend between first and second.
	"""
	res = []
	last_point = points[0]
	# up until first point
	while len(res) < last_point:
		res.append([last_point, 0, last_point])
	for pt in points:
		# up until this point
		while len(res) < pt:
			# the hard part
			val_a = float(last_point)
			val_h = float(len(res))
			val_b = float(pt)
			# linear light correction
			# don't worry about the scales they'll be fine
			if gamma:
				# testing with 1-bit output revealed 2.2 is too far
				val_a = pow(val_a, 2.15)
				val_h = pow(val_h, 2.15)
				val_b = pow(val_b, 2.15)
			# continue
			fraction = max(0, min(1, (val_h - val_a) / (val_b - val_a)))
			res.append([last_point, fraction, pt])
		last_point = pt
	# extend last point
	while len(res) < 256:
		res.append([last_point, 0, last_point])
	assert len(res) == 256
	return res

# Bitcopy tables for varying amounts of bits.
BITCOPY_MAPPROB_TABLES = [dither_point_mapprob_list(v, True) for v in BITCOPY_TABLES]

# ---- Dithering ----

class DitherBitPattern():
	"""
	A bitpattern for dithering.
	"""
	def __init__(self, content: list):
		self.width = len(content[0])
		self.height = len(content)
		self.content = content
		total = 0
		occupancy = 0
		for row in self.content:
			for v in row:
				total += 1
				occupancy += v
		self.value = (occupancy * 255) // total
	def sample(self, x, y) -> bool:
		"""
		Returns the value (True or False) at the given position.
		"""
		return self.content[y % self.height][x % self.width]

# some of the more obvious patterns
DITHER_BP_ZERO = DitherBitPattern([[0]])
DITHER_BP_ONE = DitherBitPattern([[1]])
DITHER_BP_CHECKER = DitherBitPattern([
	[1, 0],
	[0, 1]
])

def dither_compile_bit_pattern_set(patterns: list):
	"""
	Compiles a set of 256 patterns from a list.
	The patterns are measured to determine the best places for them.
	Each entry is as follows:
	[first, prob, second] - where first and second are patterns.
	"""
	ppt = []
	pdc = {}
	for p in patterns:
		pdc[p.value] = p
		ppt.append(p.value)
	ppt.sort()
	pmap = dither_point_mapprob_list(ppt, False)
	pset = []
	for i in range(256):
		mapprob = pmap[i]
		pset.append([pdc[mapprob[0]], mapprob[1], pdc[mapprob[2]]])
	return pset

DITHER_PATTERN_SET_CHECKERS = dither_compile_bit_pattern_set([
	DITHER_BP_ZERO,
	DITHER_BP_CHECKER,
	DITHER_BP_ONE
])

DITHER_PATTERN_SET_ALWAYS_CHECKERS = dither_compile_bit_pattern_set([
	DITHER_BP_CHECKER
])

def dither_compile_bayer_bit_pattern_set(array):
	"""
	Given a Bayer "order" 2D array-of-arrays, turns this into a list of patterns which is then compiled into a set.
	"""
	res = []
	for i in range(16):
		v = i + 1
		rows = []
		for irow in array:
			orow = []
			for minv in irow:
				if v >= minv:
					orow.append(0)
				else:
					orow.append(1)
			rows.append(orow)
		res.append(DitherBitPattern(rows))
	return dither_compile_bit_pattern_set(res)

# From DHALF.txt, source https://github.com/SixLabors/ImageSharp/blob/main/src/ImageSharp/Processing/Processors/Dithering/DHALF.TXT
DITHER_PATTERN_SET_BAYER4 = dither_compile_bayer_bit_pattern_set([
	[ 1,  9,  3, 11],
	[13,  5, 15,  7],
	[ 4, 12,  2, 10],
	[16,  8, 14,  6]
])

DITHER_PATTERN_SET_BAYER2 = dither_compile_bayer_bit_pattern_set([
	[1, 3],
	[4, 2]
])

DITHER_PATTERN_SETS = {
	"checkers": DITHER_PATTERN_SET_CHECKERS,
	"always-checkers": DITHER_PATTERN_SET_ALWAYS_CHECKERS,
	"bayer2": DITHER_PATTERN_SET_BAYER2,
	"bayer4": DITHER_PATTERN_SET_BAYER4,
}

def dither_bitpattern(w: int, h: int, data, values: list, mask: int, patterns: list):
	"""
	Dithers a channel using:
	+ a given list of value mapprobs
	+ a given list of pattern mapprobs
	+ a mask (to ensure values comply)
	(See dither_compile_bit_pattern_set)
	The list must be 256 entries long.
	Each entry is a DitherBitPattern.
	"""
	for i in range(len(data)):
		vmapprob = values[data[i]]
		vmapfrac = min(255, max(0, int(vmapprob[1] * 255)))
		pmapprob = patterns[vmapfrac]
		pmi = 0
		if pmapprob[1] >= 0.5:
			pmi = 2
		if pmapprob[pmi].sample(i % w, i // w) != 0:
			data[i] = vmapprob[2] & mask
		else:
			data[i] = vmapprob[0] & mask

def dither_bitpattern_random(w: int, h: int, data, values: list, mask: int, patterns: list):
	"""
	Like dither_bitpattern, but interpolates between patterns using randomness.
	"""
	for i in range(len(data)):
		vmapprob = values[data[i]]
		vmapfrac = min(255, max(0, int(vmapprob[1] * 255)))
		pmapprob = patterns[vmapfrac]
		pattern = pmapprob[0]
		if random.random() < pmapprob[1]:
			pattern = pmapprob[2]
		if pattern.sample(i % w, i // w) != 0:
			data[i] = vmapprob[2] & mask
		else:
			data[i] = vmapprob[0] & mask

def dither_channel(w: int, h: int, data, bits: int, strategy: str):
	"""
	Dithers a channel to a given number of bits with a given strategy.
	Note that this process still works in 8-bit integer space.
	Numbers are guaranteed to be clipped to, say, XXXX0000 for bits = 4.
	data is a sequence of integers between 0 and 255 inclusive.
	(This is only tested on lists!)
	"""
	# Common numbers
	# This masks such that 1 bit is 0x80, 8 is 0xFF.
	mask = (0xFF00 >> bits) & 0xFF
	# This mask is used to check the half-way threshold.
	nudge_mask = 0x80 >> bits
	# This is the amount of distance between jumps
	nudge = 0x100 >> bits
	# Strategies!
	if strategy == "floor":
		for i in range(len(data)):
			data[i] &= mask
	elif strategy == "nearest":
		# Nearest pixel value
		mapprob = BITCOPY_MAPPROB_TABLES[bits]
		for i in range(len(data)):
			mp = mapprob[data[i]]
			if mp[1] >= 0.5:
				data[i] = mp[2] & mask
			else:
				data[i] = mp[0] & mask
	elif strategy == "debug1bit":
		# Testing only.
		for i in range(len(data)):
			if data[i] >= 128:
				data[i] = 0xFF & mask
			else:
				data[i] = 0
	elif strategy == "random-floor":
		# Random dither with floored values
		nudge_m1 = nudge - 1
		for i in range(len(data)):
			data[i] = min(data[i] + random.randint(0, nudge_m1), 0xFF) & mask
	elif strategy == "random":
		# Random dither
		mapprob = BITCOPY_MAPPROB_TABLES[bits]
		for i in range(len(data)):
			vmapprob = mapprob[data[i]]
			if random.random() < vmapprob[1]:
				data[i] = vmapprob[2] & mask
			else:
				data[i] = vmapprob[0] & mask
	elif strategy == "random-borked":
		# Random dither trying to compensate
		nudge_m1 = (nudge >> 1) - 1
		error = 0
		for i in range(len(data)):
			orig = data[i]
			modified = orig - error
			v = max(min(modified + random.randint(-nudge_m1, nudge_m1), 0xFF), 0) & mask
			data[i] = v
			# simulate DD & libs16 bit copying
			simulated = v | (v >> bits)
			error = simulated - orig
	elif strategy in DITHER_PATTERN_SETS:
		dither_bitpattern(w, h, data, BITCOPY_MAPPROB_TABLES[bits], mask, DITHER_PATTERN_SETS[strategy])
	elif strategy.endswith("-random") and (strategy[0:-7] in DITHER_PATTERN_SETS):
		dither_bitpattern_random(w, h, data, BITCOPY_MAPPROB_TABLES[bits], mask, DITHER_PATTERN_SETS[strategy[0:-7]])
	else:
		raise Exception("Unsupported dithering strategy '" + strategy + "'")
