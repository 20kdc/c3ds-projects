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
BITCOPY_MAPPROB_TABLES_GAMMA = [dither_point_mapprob_list(v, True) for v in BITCOPY_TABLES]
BITCOPY_MAPPROB_TABLES_DIRECT = [dither_point_mapprob_list(v, False) for v in BITCOPY_TABLES]

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
	width = len(array[0])
	height = len(array)
	values = width * height
	for i in range(values):
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

# from Tomeno!
DITHER_PATTERN_SET_BLUE_NOISE_9X9 = dither_compile_bayer_bit_pattern_set([
	[56,  7, 18, 67, 55, 12, 34, 20, 52],
	[77, 61, 36, 51, 25, 63, 79, 71, 15],
	[24, 40,  2, 75, 29, 42,  8, 48, 32],
	[44, 11, 70, 46, 14, 21, 60,  4, 68],
	[28, 64, 19, 78, 57, 39, 73, 35, 54],
	[13, 50, 33,  5, 53,  9, 65, 17, 80],
	[59, 37, 72, 62, 26, 31, 49, 23,  1],
	[74, 10, 22, 43, 16, 69, 76, 45, 41],
	[66, 30, 47, 81,  3, 38, 58,  6, 27],
])

# from Tomeno!
DITHER_PATTERN_SET_BLUE_NOISE_15X15 = dither_compile_bayer_bit_pattern_set([
	[ 80,   9, 146, 190,  99, 216, 113, 151,  67,  88, 189, 142, 111,  60, 125],
	[ 18,  91, 222,  59, 161,  45, 141,  56, 104,  23, 154,   5,  74,  30, 155],
	[194, 166, 128,  24,  85, 122, 181,  31, 223, 174,  39,  97, 218, 179, 209],
	[103,  66,  37, 176, 207,  14, 197,  79, 159, 116, 202, 135,  84, 120,  44],
	[  7, 115, 201, 143, 106,  70, 133,  94,   8,  49,  64, 191,  15,  55, 149],
	[137, 183,  77,  53,   2, 168,  42, 213, 184, 145, 130,  27, 163, 175,  90],
	[ 33,  21, 158,  96, 220, 150, 119,  22, 101,  75, 217, 110,  71, 198, 224],
	[ 62, 208, 131, 188,  28,  63, 192,  54, 160,  32, 171,   4,  41, 100, 124],
	[ 81, 167,  47, 114,  89, 173, 136,  82, 206, 126,  87, 210, 140, 153,  12],
	[107, 144,  16,  72,  36, 214,   6, 108,  17, 182,  58, 112, 187,  51, 203],
	[ 93, 193, 221, 180, 152, 123, 199,  68, 147,  46, 156,  13,  76,  29, 177],
	[ 38,  57, 134,  10, 102,  48, 165,  26, 225,  95, 196, 132, 219, 121, 162],
	[ 69, 118,  25,  83, 204,  61,  92, 138, 117, 178,  34,  65,  86, 148,   1],
	[200, 172, 212, 157, 129, 185, 169,  40,  78,  11, 164, 105,  20, 215,  98],
	[139,  50, 109,  35,  73,  19,   3, 195, 211, 127,  52, 205, 170,  43, 186],
])

DITHER_PATTERN_SETS = {
	"checkers": DITHER_PATTERN_SET_CHECKERS,
	"always-checkers": DITHER_PATTERN_SET_ALWAYS_CHECKERS,
	"bayer2": DITHER_PATTERN_SET_BAYER2,
	"bayer4": DITHER_PATTERN_SET_BAYER4,
	"bluenoise9": DITHER_PATTERN_SET_BLUE_NOISE_9X9,
	"bluenoise15": DITHER_PATTERN_SET_BLUE_NOISE_15X15
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

def dither_channel(w: int, h: int, data, bits: int, strategy: str, is_alpha: bool):
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
	mapprob = BITCOPY_MAPPROB_TABLES_GAMMA[bits]
	if is_alpha:
		mapprob = BITCOPY_MAPPROB_TABLES_DIRECT[bits]
	# Strategies!
	if strategy == "floor":
		for i in range(len(data)):
			data[i] &= mask
	elif strategy == "nearest":
		# Nearest pixel value
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
		dither_bitpattern(w, h, data, mapprob, mask, DITHER_PATTERN_SETS[strategy])
	elif strategy.endswith("-random") and (strategy[0:-7] in DITHER_PATTERN_SETS):
		dither_bitpattern_random(w, h, data, mapprob, mask, DITHER_PATTERN_SETS[strategy[0:-7]])
	else:
		raise Exception("Unsupported dithering strategy '" + strategy + "'")
