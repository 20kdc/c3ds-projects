#!/usr/bin/env python3
# S16/C16 library for Python 3
# When copying, it is recommended you write down the commit hash here:
# ________________________________________

# caosprox - CPX server reference implementation
# Written starting in 2022 by contributors (see CREDITS.txt)
# To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
# You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

import io
import struct
import array
import PIL.Image
import random

# ---- Constants ----

# NOTE: Do not use in critical places, just comment
# Masked/transparent colour
COL_MASK = 0
# Definitive black colour (as actual blank = transparent)
COL_BLACK = 0x0020

# ---- Data Structures ----

struct_cs16_header = struct.Struct("<IH")
struct_cs16_frame = struct.Struct("<IHH")
struct_cs16_pixel = struct.Struct("<H")
struct_cs16_lofs = struct.Struct("<I")

struct_blk_header = struct.Struct("<IHHH")

# short enc/dec
def _decode_shorts(b: bytes, conv_555_565: bool, expected: int) -> array.array:
	"""
	Decodes a list of shorts as fast as possible.
	For error correction, will pads/cuts to the given expected size.
	This is not done as fast as possible.
	Note that this returns array.array, so you should use slice assignment if
	 putting this into an S16Image.
	"""
	arr = array.array("H", b)
	if sys.byteorder != "little":
		arr = arr.byteswap()
	if conv_555_565:
		for i in range(len(arr)):
			v = arr[i]
			# cheeky!
			#      ----____----____
			# 555: 0RRRRRGGGGGBBBBB
			# 565: RRRRRGGGGGgBBBBB
			# first moves R/G source fields up 1 to match 565
			# second copies upper G bit
			# third handles B
			v = ((v & 0x7FE0) << 1) | ((v & 0x0200) >> 4) | (v & 0x001F)
			arr[i] = v
	while len(arr) < expected:
		arr.append(0)
	return arr[0:expected]

def _encode_shorts(shorts) -> bytes:
	"""
	Encodes a list of shorts as fast as possible.
	"""
	arr = array.array("H", shorts)
	if sys.byteorder != "little":
		arr = arr.byteswap()
	return arr.tobytes()

# ---- S16/C16 IO ----

class S16Image():
	"""
	RGB565 image. Note that RGB555 is converted to RGB565 during load.
	"""
	def __init__(self, w: int, h: int, data = None):
		self.width = w
		self.height = h
		if data == None:
			self.data = [0] * (w * h)
		else:
			self.data = data

	def copy(self):
		"""
		Creates a copy of this image.
		"""
		return S16Image(self.width, self.height, self.data.copy())

	def to_pil(self, alpha_aware: bool = True):
		"""
		Converts the image to a PIL image.
		If alpha_aware is False, mask colour is totally disrespected.
		"""
		target = self
		pixseq = [(0, 0, 0, 0)] * (target.width * target.height)
		if alpha_aware:
			for i in range(target.width * target.height):
				v = target.data[i]
				if v != 0: # COL_MASK
					#     5 11                  3
					r = (v & 0xF800) >> 8
					r |= r >> 5
					#     6  5                  2
					g = (v & 0x07E0) >> 3
					g |= g >> 6
					#     5  0                  3
					b = (v & 0x001F) << 3
					b |= b >> 5
					pixseq[i] = (r, g, b, 255)
		else:
			for i in range(target.width * target.height):
				v = target.data[i]
				#     5 11                  3
				r = (v & 0xF800) >> 8
				r |= r >> 5
				#     6  5                  2
				g = (v & 0x07E0) >> 3
				g |= g >> 6
				#     5  0                  3
				b = (v & 0x001F) << 3
				b |= b >> 5
				pixseq[i] = (r, g, b, 255)
		img = PIL.Image.new("RGBA", (target.width, target.height))
		img.putdata(pixseq)
		return img

	def getpixel(self, x: int, y: int):
		"""
		Gets a specific pixel from the image as a short.
		Returns 0 (transparent) if out of range.
		"""
		if x < 0 or x >= self.width or y < 0 or y >= self.height:
			return 0 # COL_MASK
		return self.data[x + (y * self.width)]

	def putpixel(self, x: int, y: int, colour: int):
		"""
		Sets a specific pixel from the image as a short.
		Does nothing if out of range.
		"""
		if x < 0 or x >= self.width or y < 0 or y >= self.height:
			return
		self.data[x + (y * self.width)] = colour

	def crop_pad(self, crop_box: tuple, pad_box: tuple, pad_colour: int = 0):
		"""
		Crops and pads the image.
		"""
		self.crop_pad_x(crop_box[0], crop_box[2], pad_box[0], pad_box[2], pad_colour)
		self.crop_pad_y(crop_box[1], crop_box[3], pad_box[1], pad_box[3], pad_colour)

	def crop_pad_x(self, crop_l: int, crop_r: int, pad_l: int, pad_r: int, pad_colour: int = 0):
		"""
		Crops and pads the image on the X axis.
		"""
		newdata = []
		ptr = 0
		for i in range(self.height):
			line = self.data[ptr:ptr + self.width]
			# done as a separate step for checking reasons
			line = line[crop_l:crop_r]
			# done as a separate step for clarity reasons
			line = ([pad_colour] * pad_l) + line + ([pad_colour] * pad_r)
			newdata.extend(line)
			ptr += self.width
		self.data = newdata
		self.width = (crop_r - crop_l) + pad_l + pad_r

	def crop_pad_y(self, crop_u: int, crop_d: int, pad_u: int, pad_d: int, pad_colour: int = 0):
		"""
		Crops and pads the image on the Y axis.
		"""
		self.data = self.data[(crop_u * self.width):(crop_d * self.width)]
		self.height = crop_d - crop_u
		self.data = ([pad_colour] * (pad_u * self.width)) + self.data + ([pad_colour] * (pad_d * self.width))
		self.height += pad_u + pad_d

	def shift(self, x: int, y: int, pad_colour: int = 0):
		"""
		Shifts the image by given values.
		Negative values will delete pixels while positive values will pad the top-left with transparency.
		"""
		self.shift_x(x, pad_colour)
		self.shift_y(y, pad_colour)

	def shift_x(self, amount: int, pad_colour: int = 0):
		"""
		Shifts the image by the given X value.
		Negative values will delete pixels while positive values will pad the left with transparency.
		"""
		if amount < 0:
			if amount <= -self.width:
				self.crop_pad_x(0, 0, 0, 0, pad_colour)
			else:
				self.crop_pad_x(-amount, self.width, 0, 0, pad_colour)
		elif amount > 0:
			self.crop_pad_x(0, self.width, amount, 0, pad_colour)

	def shift_y(self, amount: int, pad_colour: int = 0):
		"""
		Shifts the image by the given Y value.
		Negative values will delete pixels while positive values will pad the top with transparency.
		"""
		if amount < 0:
			if amount <= -self.height:
				self.crop_pad_y(0, 0, 0, 0, pad_colour)
			else:
				self.crop_pad_y(-amount, self.height, 0, 0, pad_colour)
		elif amount > 0:
			self.crop_pad_y(0, self.height, amount, 0, pad_colour)

	def colours_from(self, srci, srcx: int, srcy: int):
		"""
		Transfers colours from a given source image.
		"""
		idx = 0
		for y in range(self.height):
			for x in range(self.width):
				sv = srci.getpixel(srcx + x, srcy + y)
				v = self.data[idx]
				if v != 0: # COL_MASK
					v = sv
					if v == 0: # COL_MASK
						v = 0x0020 # COL_BLACK
				self.data[idx] = v
				idx = idx + 1

	def blit(self, srci, tx: int, ty: int, alpha_aware: bool = True):
		"""
		Blits the source image to the given destination.
		Note that alpha_aware can be set to false in which case transparency is a lie and so forth.
		Also be aware this function won't error if the image goes off the borders.
		"""
		# create in-range arrays
		# this is still more efficient than doing the checks on every pixel
		xir = []
		for x in range(srci.width):
			rx = x + tx
			if rx >= 0 and rx < self.width:
				xir.append(x)
		yir = []
		for y in range(srci.height):
			ry = y + ty
			if ry >= 0 and ry < self.height:
				yir.append(y)
		# continue
		for y in yir:
			src_row = y * srci.width
			dst_row = ((ty + y) * self.width) + tx
			# inner loop, optimize
			if alpha_aware:
				for x in xir:
					v = srci.data[src_row + x]
					if v != 0:
						self.data[dst_row + x] = v
			else:
				for x in xir:
					col = srci.data[src_row + x]
					self.data[dst_row + x] = col

# ---- S16/C16 IO ----

def is_555(data: bytes) -> bool:
	"""
	Checks if a file is RGB555.
	"""
	filetype, count = struct_cs16_header.unpack_from(data, 0)
	if filetype == 0:
		return True
	elif filetype == 1:
		return False
	elif filetype == 2:
		return True
	elif filetype == 3:
		return False
	else:
		raise Exception("Unknown S16/C16 magic number " + str(filetype))

def is_c16(data: bytes) -> bool:
	"""
	Checks if a file is a C16 file.
	(This is meant to help with the mask command, which should save whatever it got.)
	"""
	filetype, count = struct_cs16_header.unpack_from(data, 0)
	if filetype == 0:
		return False
	elif filetype == 1:
		return False
	elif filetype == 2:
		return True
	elif filetype == 3:
		return True
	else:
		raise Exception("Unknown S16/C16 magic number " + str(filetype))

def decode_cs16(data: bytes) -> list:
	"""
	Decodes a C16 or S16 file.
	Returns a list of S16Image objects.
	"""
	filetype, count = struct_cs16_header.unpack_from(data, 0)
	is_compressed = False
	is_555 = False
	if filetype == 0:
		is_compressed = False
		is_555 = True
	elif filetype == 1:
		is_compressed = False
		is_555 = False
	elif filetype == 2:
		is_compressed = True
		is_555 = True
	elif filetype == 3:
		is_compressed = True
		is_555 = False
	else:
		raise Exception("Unknown S16/C16 magic number " + str(filetype))
	hptr = 6
	results = []
	for i in range(count):
		iptr, iw, ih = struct_cs16_frame.unpack_from(data, hptr)
		img = S16Image(iw, ih)
		if is_compressed:
			# 8 + (4 * (ih - 1))
			hptr += 4 * (ih + 1)
			# have to decode things in lines. how can we cheese this for max. efficiency?
			for row in range(ih):
				pixidx = row * iw
				while True:
					elm = struct_cs16_pixel.unpack_from(data, iptr)[0]
					iptr += 2
					if elm == 0:
						break
					runlen = elm >> 1
					if (elm & 1) != 0:
						niptr = iptr + (runlen * 2)
						img.data[pixidx:pixidx + runlen] = _decode_shorts(data[iptr:niptr], is_555, runlen)
						pixidx += runlen
						iptr = niptr
					else:
						pixidx += runlen
		else:
			hptr += 8
			# just decode everything at once
			iw = iw * ih
			iw2 = iw * ih * 2
			img.data[0:iw] = _decode_shorts(data[iptr:iptr + iw2], is_555, iw)
		results.append(img)
	return results

def encode_s16(images) -> bytes:
	"""
	Encodes a S16 file from S16Image objects.
	The S16 file is forced to be RGB565.
	Returns bytes.
	"""
	data = struct_cs16_header.pack(1, len(images))
	blob_ptr = struct_cs16_header.size + (struct_cs16_frame.size * len(images))
	blob = b""
	for v in images:
		data += struct_cs16_frame.pack(blob_ptr, v.width, v.height)
		blob += _encode_shorts(v.data)
		blob_ptr += len(v.data) * 2
	return data + blob

def _encode_c16_run(line_p, run):
	if len(run) == 0:
		return
	if run[0] == 0: # COL_MASK
		# transparent run
		line_p.append(len(run) << 1)
	else:
		# not-transparent run
		line_p.append((len(run) << 1) | 1)
		for v in run:
			line_p.append(v)

def _encode_c16_line(data, ofs, l):
	line_p = []
	run = []
	run_tr = False
	for i in range(l):
		v = data[ofs]
		vtr = v == 0 # COL_MASK
		# break run on change, or if the run is already 16383 pixels long
		if (vtr != run_tr) or (len(run) >= 0x3FFF):
			_encode_c16_run(line_p, run)
			run = []
			run_tr = vtr
		# add to run
		run.append(v)
		ofs += 1
	# encode last run (if any)
	_encode_c16_run(line_p, run)
	# end of line marker
	line_p.append(0)
	# pack into bytes
	return _encode_shorts(line_p)

def encode_c16(images) -> bytes:
	"""
	Encodes a C16 file from S16Image objects.
	The C16 file is forced to be RGB565.
	Returns bytes.
	"""
	data = struct_cs16_header.pack(3, len(images))
	blob = b""
	# calculate initial blob_ptr, including line offsets
	# this must equal the length of data
	blob_ptr = struct_cs16_header.size + (struct_cs16_frame.size * len(images))
	for v in images:
		if v.height > 0:
			blob_ptr += (v.height - 1) * 4
	expected_data_len = blob_ptr
	# actually build
	for v in images:
		data += struct_cs16_frame.pack(blob_ptr, v.width, v.height)
		for y in range(v.height):
			if y != 0:
				data += struct_cs16_lofs.pack(blob_ptr)
			lb = _encode_c16_line(v.data, y * v.width, v.width)
			blob += lb
			blob_ptr += len(lb)
		# EOI marker
		blob += struct_cs16_pixel.pack(0)
		blob_ptr += 2
	assert expected_data_len == len(data)
	return data + blob

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

def dither_point_nearest_list(points: list):
	"""
	Given a list of 0-255 ints, creates a list mapping 256 values to the nearest.
	"""
	lst = []
	for i in range(256):
		nearest = 0
		nearest_dist = 512
		for v in points:
			dist = abs(v - i)
			if dist < nearest_dist:
				nearest = v
				nearest_dist = dist
		lst.append(nearest)
	return lst

def dither_point_first_list(points: list):
	"""
	Given a list of 0-255 ints, creates a list giving the index to the first
	instance of a given number.
	"""
	lst = []
	last_value = -1
	last_index = -1
	idx = 0
	for v in points:
		if v != last_value:
			lst.append(idx)
			last_value = v
			last_index = idx
		else:
			lst.append(last_index)
		idx += 1
	return lst

def dither_point_last_list(points: list):
	"""
	Given a list of 0-255 ints, creates a list giving the index to the last
	instance of a given number.
	"""
	# reverse inputs
	tmprev = list(points)
	tmprev.reverse()
	# do it
	lst = dither_point_first_list(tmprev)
	# reverse results
	lst.reverse()
	for i in range(len(lst)):
		lst[i] = len(points) - (lst[i] + 1)
	return lst

def dither_point_mapprob_list(points: list):
	"""
	Given a list of 0-255 ints, creates a list of 256 entries as follows:
	[nearest, prob, second]
	nearest and second are values from points.
	prob is the floating-point fraction to blend between nearest and second.
	prob should not reach above 0.5 (as that should change the nearest)
	"""
	nearest = dither_point_nearest_list(points)
	first = dither_point_first_list(nearest)
	last = dither_point_last_list(nearest)
	res = []
	for target in range(256):
		v = nearest[target]
		relevant_idx = target
		if target < v:
			# below
			relevant_idx = first[target] - 1
		elif target > v:
			# above
			relevant_idx = last[target] + 1
		if relevant_idx == target or relevant_idx < 0 or relevant_idx >= 256:
			# nothing to interpolate to
			res.append([v, 0.0, v])
		else:
			v2 = nearest[relevant_idx]
			if v == v2:
				raise Exception("Not supposed to happen: " + str(v) + " " + str(v2) + " " + str(target) + " " + str(relevant_idx))
			dist_to_relevant = float(abs(v - v2))
			dist_to_target = float(abs(v - target))
			res.append([v, dist_to_target / dist_to_relevant, v2])
	return res

# Bitcopy tables for varying amounts of bits.
BITCOPY_MAPPROB_TABLES = [dither_point_mapprob_list(v) for v in BITCOPY_TABLES]

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
		self.value = (occupancy * 256) // total
	def sample(self, x, y):
		return self.content[y % self.height][x % self.width]

def dither_compile_bit_pattern_set(patterns: list):
	"""
	Compiles a set of 256 patterns from a list.
	The patterns are measured to determine the best places for them.
	Each entry is as follows:
	[nearest, prob, second] - where nearest and second are patterns.
	"""
	ppt = []
	pdc = {}
	for p in patterns:
		pdc[p.value] = p
		ppt.append(p.value)
	pmap = dither_point_mapprob_list(ppt)
	pset = []
	for i in range(256):
		mapprob = pmap[i]
		pset.append([pdc[mapprob[0]], mapprob[1], pdc[mapprob[2]]])
	return pset

DITHER_PATTERN_SET_HEXCHECKERS = dither_compile_bit_pattern_set([
	DitherBitPattern([
		[0],
	]),
	DitherBitPattern([
		#     | shift down 2
		[1, 0, 0, 0],
		[0, 0, 0, 0],
		[0, 0, 1, 0],
		[0, 0, 0, 0],
	]),
	DitherBitPattern([
		#     | shift down 1
		[1, 0, 0, 0],
		[0, 0, 1, 0],
	]),
	DitherBitPattern([
		#          | shift down 2
		[1, 0, 0, 0, 0, 1, 0, 1],
		[0, 0, 1, 0, 1, 0, 1, 0],
		[0, 1, 0, 1, 1, 0, 0, 0],
		[1, 0, 1, 0, 0, 0, 1, 0],
	]),
	DitherBitPattern([
		[1, 0],
		[0, 1],
	]),
	DitherBitPattern([
		[0, 0, 1, 1],
		[1, 1, 0, 1],
		[1, 1, 0, 0],
		[0, 1, 1, 1],
	]),
	DitherBitPattern([
		#     | shift down 1
		[0, 1, 1, 1],
		[1, 1, 0, 1],
	]),
	DitherBitPattern([
		#     | shift down 2
		[0, 1, 1, 1],
		[1, 1, 1, 1],
		[1, 1, 0, 1],
		[1, 1, 1, 1],
	]),
	DitherBitPattern([
		[1],
	]),
])

DITHER_PATTERN_SET_CHECKERS = dither_compile_bit_pattern_set([
	DitherBitPattern([
		[0]
	]),
	DitherBitPattern([
		[1, 0],
		[0, 1],
	]),
	DitherBitPattern([
		[1]
	])
])

DITHER_PATTERN_SET_ALWAYS_CHECKERS = dither_compile_bit_pattern_set([
	DitherBitPattern([
		[1, 0],
		[0, 1],
	])
])

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
		if pmapprob[0].sample(i % w, i // w) != 0:
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
			data[i] = mapprob[data[i]][0] & mask
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
	elif strategy == "hexcheckers":
		dither_bitpattern(w, h, data, BITCOPY_MAPPROB_TABLES[bits], mask, DITHER_PATTERN_SET_HEXCHECKERS)
	elif strategy == "hexcheckers-random":
		dither_bitpattern_random(w, h, data, BITCOPY_MAPPROB_TABLES[bits], mask, DITHER_PATTERN_SET_HEXCHECKERS)
	elif strategy == "checkers":
		dither_bitpattern(w, h, data, BITCOPY_MAPPROB_TABLES[bits], mask, DITHER_PATTERN_SET_CHECKERS)
	elif strategy == "checkers-random":
		dither_bitpattern_random(w, h, data, BITCOPY_MAPPROB_TABLES[bits], mask, DITHER_PATTERN_SET_CHECKERS)
	elif strategy == "always-checkers":
		dither_bitpattern(w, h, data, BITCOPY_MAPPROB_TABLES[bits], mask, DITHER_PATTERN_SET_ALWAYS_CHECKERS)
	else:
		raise Exception("Unsupported dithering strategy '" + strategy + "'")

def dither_565(w: int, h: int, data_r, data_g, data_b, cdmode: str):
	"""
	Dithers an image to RGB565.
	The data is given as modifiable sequence-like objects.
	(This is only tested on lists!)
	This is done using the given strategy and modes.
	See dither_channel for precise control and output details.
	"""
	dither_channel(w, h, data_r, 5, cdmode)
	dither_channel(w, h, data_g, 6, cdmode)
	dither_channel(w, h, data_b, 5, cdmode)

CDMODE_DEFAULT = "floor"
ADMODE_DEFAULT = "nearest"

def pil_to_565(pil: PIL.Image, false_black: int = COL_BLACK, cdmode: str = CDMODE_DEFAULT, admode: str = ADMODE_DEFAULT) -> S16Image:
	"""
	Encodes a PIL.Image into a 565 S16Image.
	Pixels that would be "accidentally transparent" are nudged to false_black.
	cdmode and admode are dither modes as per the dither function.
	These are for colours and alpha respectively.
	"""
	img = S16Image(pil.width, pil.height)
	pil = pil.convert("RGBA")
	idx = 0
	data_r = list(pil.getdata(0))
	data_g = list(pil.getdata(1))
	data_b = list(pil.getdata(2))
	data_a = list(pil.getdata(3))
	# skip the full dithering pass if we implement it ourselves
	if cdmode != "floor":
		dither_565(pil.width, pil.height, data_r, data_g, data_b, cdmode)
	if admode != "nearest":
		dither_channel(pil.width, pil.height, data_a, 1, admode)
	for i in range(pil.width * pil.height):
		v = 0
		if data_a[i] >= 128:
			v = ((data_r[i] << 8) & 0xF800) | ((data_g[i] << 3) & 0x07E0) | ((data_b[i] >> 3) & 0x001F)
			if v == 0: # COL_MASK
				v = false_black
		img.data[idx] = v
		idx += 1
	return img

def pil_to_565_blk(pil: PIL.Image, cdmode: str = "floor") -> S16Image:
	"""
	Encodes a PIL.Image into a 565 S16Image, assuming it will be a BLK file.
	Therefore, alpha and collisions with the masking colour are ignored.
	cdmode is a dither mode as per the dither function.
	"""
	img = S16Image(pil.width, pil.height)
	pil = pil.convert("RGB")
	idx = 0
	data_r = list(pil.getdata(0))
	data_g = list(pil.getdata(1))
	data_b = list(pil.getdata(2))
	dither_565(pil.width, pil.height, data_r, data_g, data_b, cdmode)
	for i in range(pil.width * pil.height):
		v = ((data_r[i] << 8) & 0xF800) | ((data_g[i] << 3) & 0x07E0) | ((data_b[i] >> 3) & 0x001F)
		img.data[idx] = v
		idx += 1
	return img

# ---- BLK ----

def encode_blk_blocks(images, blocks_w: int) -> bytes:
	"""
	Encodes a BLK file from S16Image objects (128x128 tiles).
	The BLK file is forced to be RGB565.
	Returns bytes.
	"""
	# Encode, but then change the header.
	# This is probably the official procedure which is why the offsets are off by 4.
	tmp = encode_s16(images)[struct_cs16_header.size:]
	return struct_blk_header.pack(1, blocks_w, len(images) // blocks_w, len(images)) + tmp

def encode_blk(image: S16Image) -> bytes:
	"""
	Encodes a BLK file from an S16Image object.
	The BLK file is forced to be RGB565.
	Returns bytes.
	"""
	# Calculate necessary blocks
	blocks_w = image.width >> 7
	blocks_h = image.height >> 7
	if (image.width & 127) != 0:
		blocks_w += 1
	if (image.height & 127) != 0:
		blocks_h += 1
	tiles = []
	for x in range(blocks_w):
		for y in range(blocks_h):
			tile = S16Image(128, 128)
			tile.blit(image, x * -128, y * -128, False)
			tiles.append(tile)
	return encode_blk_blocks(tiles, blocks_w)

def decode_blk_blocks(data: bytes):
	"""
	Decodes a BLK file into the width in blocks, the height in blocks, and a list of tiles.
	As such, returns three values.
	"""
	filetype, blocks_w, blocks_h, blocks_total = struct_blk_header.unpack_from(data, 0)
	iptr = struct_blk_header.size

	# Ok, so.
	# About the "Creatures 3 Room Editor" from www.smog-it.co.uk/software/c3re.
	# It doesn't even produce normal, valid BLK files, but it still
	#  appears to watermark them with an advert for itself...
	# Hence the name and shame here.
	# The tile offsets break after the first row.
	# This seems accidental, because:
	#  + Someone who actually knew offsets didn't matter would simply zero them
	#  + Backgrounds are column-major, indicating an attempt to write accurate
	#     offsets
	# If you want a victim, get Random's Room off of the EemFoo archive.

	# That in mind, we can no longer change header and pass to decode_cs16.
	# Instead, we have to base things on how C3 *actually* handles backgrounds:
	# Read everything assuming the exact order the Map Editor would write it
	#  with no padding.

	# Check header
	is_555 = False
	if filetype == 0:
		is_compressed = False
		is_555 = True
	elif filetype == 1:
		is_compressed = False
		is_555 = False
	else:
		raise Exception("Unknown BLK magic number " + str(filetype))

	# Read sprite headers, but we don't *actually* care about pointers
	results = []
	for i in range(blocks_total):
		xptr, iw, ih = struct_cs16_frame.unpack_from(data, iptr)
		img = S16Image(iw, ih)
		results.append(img)
		iptr += struct_cs16_frame.size

	# Read the actual sprites
	for img in results:
		# just decode everything at once
		iw = img.width * img.height
		iw2 = iw * 2
		img.data[0:iw] = _decode_shorts(data[iptr:iptr + iw2], is_555, iw)
		iptr += iw2

	return blocks_w, blocks_h, results

def decode_blk(data: bytes) -> S16Image:
	"""
	Decodes a BLK file into a full image.
	"""
	blocks_w, blocks_h, blocks = decode_blk_blocks(data)
	full = S16Image(blocks_w * 128, blocks_h * 128)
	idx = 0
	for x in range(blocks_w):
		for y in range(blocks_h):
			full.blit(blocks[idx], x * 128, y * 128, False)
			idx += 1
	return full

# ---- Command Line ----

if __name__ == "__main__":

	def command_help():
		print("libs16.py info <IN>")
		print(" information on a c16/s16 file")
		print("libs16.py encodeS16 <INDIR> <OUT> [<CDMODE> [<ADMODE>]]")
		print(" encodes 565 S16 file from directory")
		print("libs16.py encodeC16 <INDIR> <OUT> [<CDMODE> [<ADMODE>]]")
		print(" encodes 565 C16 file from directory")
		print("libs16.py encodeBLK <IN> <OUT> [<CDMODE>]")
		print(" encodes 565 BLK file from source")
		print("libs16.py decode <IN> <OUTDIR>")
		print(" decodes S16 or C16 files")
		print("libs16.py decodeFrame <IN> <FRAME> <OUT>")
		print(" decodes a single frame of a S16/C16 to a PNG file")
		print("libs16.py decodeBLK <IN> <OUT>")
		print(" decodes a BLK file to a PNG file")
		print("libs16.py mask <SOURCE> <X> <Y> <VICTIM> <FRAME> [<CHECKPRE> [<CHECKPOST>]]")
		print(" **REWRITES** the given FRAME of the VICTIM file to use the colours from SOURCE frame 0 at the given X/Y position, but basing alpha on the existing data in the frame.")
		print(" CHECKPRE/CHECKPOST are useful for comparisons.")
		print("libs16.py shift <VICTIM> <FRAME> <X> <Y> [<CHECKPRE> [<CHECKPOST>]]")
		print(" **REWRITES** the given FRAME of the VICTIM file to shift it by X and Y.")
		print(" CHECKPRE/CHECKPOST are useful for comparisons.")
		print(" If the X and Y values are *negative*, pixels are lost. If they are *positive*, transparent pixels are added.")
		print("libs16.py blit <SOURCE> <SRCFRAME> <VICTIM> <DSTFRAME> <X> <Y> [<CHECKPRE> [<CHECKPOST>]]")
		print(" **REWRITES** the given DSTFRAME of the VICTIM file, blitting SOURCE's SRCFRAME to it.")
		print(" This blit is alpha-aware.")
		print(" CHECKPRE/CHECKPOST are useful for comparisons.")
		print("libs16.py genPalRef <DST>")
		print(" generates a PNG palette reference file")
		print("libs16.py dither <IN> <OUT> [<CDMODE> [<ADMODE> [<RBITS> [<GBITS> [<BBITS> [<ABITS>]]]]]]")
		print(" tests dithering - defaults to RGB565")
		print("libs16.py dithera <IN> [<CDMODE> [<ADMODE> [<RBITS> [<GBITS> [<BBITS> [<ABITS>]]]]]]")
		print(" dither command, but output name is inferred from details")
		print("libs16.py dithercmyk <IN> [<CDMODE> [<CBITS> [<MBITS> [<YBITS> [<KBITS>]]]]]")
		print(" joke command")
		print("")
		print("s16/c16 files are converted to directories of numbered PNG files.")
		print("This process is lossless, though RGB555 files are converted to RGB565.")
		print("The inverse conversion is of course not lossless (lower bits are dropped).")
		print("CDMODE and ADMODE controls dithering and such.")
		print("Modes are:")
		print(" floor: Floors the value")
		print(" nearest: Nearest bitcopied value")
		print(" debug1bit: 1-bit")
		print(" random-floor: Random increase up to the distance between floored values, then floors")
		print(" random: Randomly picks from the two nearest values, weighted by distance.")
		print(" random-borked: A silly attempt at an error-correction-based dither that goes a little out of control")
		print("                Decent with alpha, though")
		print(" hexcheckers: Ordered \"4x4-ish\" (but tries to stick to hexagonal 2x2) dither.")
		print(" hexcheckers-random: Same, but with randomness used to blend between patterns.")
		print(" checkers: 2x2 checkerboard in the 25% to 75% range.")
		print("           Adds less than a bit of \"effective depth\", but very reliable.")
		print(" checkers-random: Same, but with the randomness blending.")
		print("                  The checkerboarding avoids some of random's problems.")
		print(" always-checkers: Checkerboard whenever the value is not exactly equal.")
		print("The default CDMODE is floor, and the default ADMODE is nearest.")
		print("(This is because these modes are lossless with decode output.)")

	def _read_bytes(fn):
		f = open(fn, "rb")
		data = f.read()
		f.close()
		return data

	def _read_cs16_file(fn):
		return decode_cs16(_read_bytes(fn))

	def _opt_save_test(fn, fr):
		if not (fn is None):
			if fn != "":
				fr.to_pil().save(fn, "PNG")

	def _opt_arg(idx, df = None):
		if len(sys.argv) <= idx:
			return df
		return sys.argv[idx]

	def _write_equal_format(fn, images, original):
		f = open(fn, "wb")
		if is_c16(original):
			f.write(encode_c16(images))
		else:
			f.write(encode_s16(images))
		f.close()

	import sys
	import os
	import os.path

	def encode_fileset(fileset, output, compressed, cdmode, admode):
		imgs = []
		idx = 0
		exts = [".png", ".jpg", ".bmp"]
		while True:
			f_path = None
			for ext in exts:
				f_path = os.path.join(fileset, str(idx) + ext)
				try:
					os.stat(f_path)
					# yay!
					break
				except:
					pass
				# :(
				f_path = None
			if f_path is None:
				# did not find a candidate
				break
			print("Frame " + f_path + "...")
			f_pil = PIL.Image.open(f_path)
			print(" Encoding to RGB565...")
			imgs.append(pil_to_565(f_pil, cdmode = cdmode, admode = admode))
			idx += 1
		print("Building final data...")
		if compressed:
			data = encode_c16(imgs)
		else:
			data = encode_s16(imgs)
		res = open(output, "wb")
		res.write(data)
		res.close()

	if len(sys.argv) >= 2:
		if sys.argv[1] == "info":
			data = _read_bytes(sys.argv[2])
			images = decode_cs16(data)
			idx = 0
			if is_555(data):
				if is_c16(data):
					print("RGB555 C16")
				else:
					print("RGB555 S16")
			elif is_c16(data):
				print("RGB565 C16")
			else:
				print("RGB565 S16")
			print(str(len(images)) + " frames")
			for v in images:
				print(" " + str(idx) + ": " + str(v.width) + "x" + str(v.height))
				idx += 1
		elif sys.argv[1] == "encodeS16" or sys.argv[1] == "encodeC16":
			cdmode = _opt_arg(4, CDMODE_DEFAULT)
			admode = _opt_arg(5, ADMODE_DEFAULT)
			encode_fileset(sys.argv[2], sys.argv[3], sys.argv[1] == "encodeC16", cdmode, admode)
		elif sys.argv[1] == "encodeBLK":
			cdmode = _opt_arg(4, "floor")
			img = PIL.Image.open(sys.argv[2])
			blk = encode_blk(pil_to_565_blk(img, cdmode = cdmode))
			f = open(sys.argv[3], "wb")
			f.write(blk)
			f.close()
		elif sys.argv[1] == "decode":
			try:
				os.mkdir(sys.argv[3])
			except:
				# we don't care
				pass
			images = _read_cs16_file(sys.argv[2])
			idx = 0
			for v in images:
				vpil = v.to_pil()
				vpil.save(os.path.join(sys.argv[3], str(idx) + ".png"), "PNG")
				idx += 1
		elif sys.argv[1] == "decodeFrame":
			frame = int(sys.argv[3])
			images = _read_cs16_file(sys.argv[2])
			vpil = images[frame].to_pil()
			vpil.save(sys.argv[4], "PNG")
		elif sys.argv[1] == "decodeBLK":
			blk = decode_blk(_read_bytes(sys.argv[2]))
			vpil = blk.to_pil(alpha_aware = False)
			vpil.save(sys.argv[3], "PNG")
		elif sys.argv[1] == "mask":
			# args
			srci = sys.argv[2]
			srcx = int(sys.argv[3])
			srcy = int(sys.argv[4])
			victim_fn = sys.argv[5]
			frame = int(sys.argv[6])
			test_pre = _opt_arg(7)
			test_post = _opt_arg(8)
			# load source image
			srci = _read_cs16_file(srci)[0]
			# load target file
			victim_data = _read_bytes(victim_fn)
			images = decode_cs16(victim_data)
			# test pre
			_opt_save_test(test_pre, images[frame])
			# actually run op
			images[frame].colours_from(srci, srcx, srcy)
			# test post
			_opt_save_test(test_post, images[frame])
			# writeout
			_write_equal_format(victim_fn, images, victim_data)
		elif sys.argv[1] == "shift":
			# args
			victim_fn = sys.argv[2]
			frame = int(sys.argv[3])
			shiftx = int(sys.argv[4])
			shifty = int(sys.argv[5])
			test_pre = _opt_arg(6)
			test_post = _opt_arg(7)
			# load target file
			victim_data = _read_bytes(victim_fn)
			images = decode_cs16(victim_data)
			# test pre
			_opt_save_test(test_pre, images[frame])
			# actually run op
			images[frame].shift(shiftx, shifty)
			# test post
			_opt_save_test(test_post, images[frame])
			# writeout
			_write_equal_format(victim_fn, images, victim_data)
		elif sys.argv[1] == "blit":
			# args
			srci = sys.argv[2]
			srcf = int(sys.argv[3])
			victim_fn = sys.argv[4]
			frame = int(sys.argv[5])
			targetx = int(sys.argv[6])
			targety = int(sys.argv[7])
			test_pre = _opt_arg(8)
			test_post = _opt_arg(9)
			# load source image
			srci = _read_cs16_file(srci)[srcf]
			# load target file
			victim_data = _read_bytes(victim_fn)
			images = decode_cs16(victim_data)
			# test pre
			_opt_save_test(test_pre, images[frame])
			# actually run op
			images[frame].blit(srci, targetx, targety)
			# finish
			_opt_save_test(test_post, images[frame])
			_write_equal_format(victim_fn, images, victim_data)
		elif sys.argv[1] == "genPalRef":
			image = S16Image(256, 256)
			idx = 0
			for y in range(256):
				for x in range(256):
					image.data[idx] = idx
					idx += 1
			vpil = image.to_pil(alpha_aware = False)
			vpil.save(sys.argv[2], "PNG")
		elif sys.argv[1] == "dither" or sys.argv[1] == "dithera":
			fni = sys.argv[2]
			fno = None
			filter_params_base = 4
			if sys.argv[1] == "dithera":
				filter_params_base = 3
			else:
				fno = sys.argv[3]
			cdmode = _opt_arg(filter_params_base, CDMODE_DEFAULT)
			admode = _opt_arg(filter_params_base + 1, ADMODE_DEFAULT)
			rbits = int(_opt_arg(filter_params_base + 2, "5"))
			gbits = int(_opt_arg(filter_params_base + 3, "6"))
			bbits = int(_opt_arg(filter_params_base + 4, "5"))
			abits = int(_opt_arg(filter_params_base + 5, "1"))
			# autogen name
			if fno == None:
				fno = os.path.join(os.path.dirname(fni), os.path.basename(fni) + "." + cdmode + str(rbits) + str(gbits) + str(bbits) + "." + admode + str(abits) + ".png")
			# alright, load
			vpil = PIL.Image.open(fni)
			vpil = vpil.convert("RGBA")
			# convert
			data_r = list(vpil.getdata(0))
			data_g = list(vpil.getdata(1))
			data_b = list(vpil.getdata(2))
			data_a = list(vpil.getdata(3))
			# dither
			dither_channel(vpil.width, vpil.height, data_r, rbits, cdmode)
			dither_channel(vpil.width, vpil.height, data_g, gbits, cdmode)
			dither_channel(vpil.width, vpil.height, data_b, bbits, cdmode)
			dither_channel(vpil.width, vpil.height, data_a, abits, admode)
			# convert
			data_total = []
			for i in range(vpil.width * vpil.height):
				r = BITCOPY_TABLES[rbits][data_r[i]]
				g = BITCOPY_TABLES[gbits][data_g[i]]
				b = BITCOPY_TABLES[bbits][data_b[i]]
				a = BITCOPY_TABLES[abits][data_a[i]]
				data_total.append((r, g, b, a))
			vpil.putdata(data_total)
			# done
			vpil.save(fno, "PNG")
		elif sys.argv[1] == "dithercmyk":
			# For fun only. PIL's RGB->CMYK conversion is a little broken.
			# It pretends K doesn't really exist.
			fni = sys.argv[2]
			cdmode = _opt_arg(3, CDMODE_DEFAULT)
			cbits = int(_opt_arg(4, "1"))
			mbits = int(_opt_arg(5, str(cbits)))
			ybits = int(_opt_arg(6, str(mbits)))
			kbits = int(_opt_arg(7, str(ybits)))
			# autogen name
			fno = os.path.join(os.path.dirname(fni), os.path.basename(fni) + ".cmyk." + cdmode + str(cbits) + str(mbits) + str(ybits) + str(kbits) + ".png")
			# alright, load
			vpil = PIL.Image.open(fni)
			vpil = vpil.convert("CMYK")
			# convert
			data_c = list(vpil.getdata(0))
			data_m = list(vpil.getdata(1))
			data_y = list(vpil.getdata(2))
			data_k = list(vpil.getdata(3))
			# Fix K???
			for i in range(vpil.width * vpil.height):
				if data_k[i] == 0:
					common = min(data_c[i], data_m[i], data_y[i])
					data_k[i] = common
					data_c[i] -= common
					data_m[i] -= common
					data_y[i] -= common
			# dither
			dither_channel(vpil.width, vpil.height, data_c, cbits, cdmode)
			dither_channel(vpil.width, vpil.height, data_m, mbits, cdmode)
			dither_channel(vpil.width, vpil.height, data_y, ybits, cdmode)
			dither_channel(vpil.width, vpil.height, data_k, kbits, cdmode)
			# convert
			data_total = []
			for i in range(vpil.width * vpil.height):
				c = BITCOPY_TABLES[cbits][data_c[i]]
				m = BITCOPY_TABLES[mbits][data_m[i]]
				y = BITCOPY_TABLES[ybits][data_y[i]]
				k = BITCOPY_TABLES[kbits][data_k[i]]
				data_total.append((c, m, y, k))
			vpil.putdata(data_total)
			# done
			# can't actually *save* as CMYK, except in EPS, which is hard to check
			vpil = vpil.convert("RGBA")
			vpil.save(fno, "PNG")
		else:
			print("cannot understand: " + sys.argv[1])
			command_help()
	else:
		command_help()

