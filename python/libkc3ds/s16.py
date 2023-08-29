#!/usr/bin/env python3
# S16/C16 library for Python 3
# When copying, it is recommended you write down the commit hash here:
# ________________________________________

# caosprox - CPX server reference implementation
# Written starting in 2022 by contributors (see CREDITS.txt)
# To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
# You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

import struct
import array
import sys

# ---- Constants ----

# NOTE: Do not use in critical places, just comment
# Masked/transparent colour
COL_MASK = 0
# Definitive black colour (as actual blank = transparent)
COL_BLACK = 0x0020

# ---- Format Registry ----

class CS16Format():
	def __init__(self, compressed, cfmt, endian, desc):
		self.compressed = compressed
		self.cfmt = cfmt
		self.endian = endian
		self.desc = desc

CS16_FILETYPES = {
	0x00000000: CS16Format(False, "rgb555",  "little", "S16 RGB555 LE"),
	0x00000001: CS16Format(False, "rgb565",  "little", "S16 RGB565 LE"),
	0x00000002: CS16Format( True, "rgb555",  "little", "C16 RGB555 LE"),
	0x00000003: CS16Format( True, "rgb565",  "little", "C16 RGB565 LE"),
	0x01000000: CS16Format(False, "rgb5551", "big",    "N16 RGB5551 BE"),
	0x03000000: CS16Format(False, "rgb5551", "big",    "M16 RGB5551 BE")
}

# ---- Data Structures ----

struct_cs16_header = struct.Struct("<IH")
struct_cs16_frame = struct.Struct("<IHH")
struct_cs16_pixel = struct.Struct("<H")
struct_cs16_lofs = struct.Struct("<I")

struct_blk_header = struct.Struct("<IHHH")

# short enc/dec
def _decode_shorts(b: bytes, cfmt: str, endian: str, expected: int) -> array.array:
	"""
	Decodes a list of shorts to RGB565 as fast as possible.
	For error correction, will pad/cut to the given expected size.
	This is not done as fast as possible.
	Note that this returns array.array, so you should use slice assignment if
	 putting this into an S16Image.
	"""
	arr = array.array("H", b)
	# endianness swap	
	if sys.byteorder != endian:
		arr = arr.byteswap()
	# RGB!
	if cfmt == "rgb565":
		pass
	elif cfmt == "rgb555":
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
	else:
		raise Exception("Unsupported colour format for decoding: " + cfmt)
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

	def to_rgb(self):
		"""
		Converts the image to a list of tuples (R, G, B).
		The components are from 0 to 255.
		"""
		pixseq = [(0, 0, 0)] * (self.width * self.height)
		for i in range(self.width * self.height):
			v = self.data[i]
			#     5 11                  3
			r = (v & 0xF800) >> 8
			r |= r >> 5
			#     6  5                  2
			g = (v & 0x07E0) >> 3
			g |= g >> 6
			#     5  0                  3
			b = (v & 0x001F) << 3
			b |= b >> 5
			pixseq[i] = (r, g, b)
		return pixseq

	def to_rgba(self):
		"""
		Converts the image to a list of tuples (R, G, B, A).
		The components are from 0 to 255.
		"""
		pixseq = [(0, 0, 0, 0)] * (self.width * self.height)
		for i in range(self.width * self.height):
			v = self.data[i]
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
		return pixseq

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
		if crop_l < 0 or crop_l > self.width or crop_r < crop_l or crop_r > self.width:
			raise Exception("Invalid crop coordinates " + str(crop_l) + ", " + str(crop_r) + " of width " + self.width)
		if pad_l < 0 or pad_r < 0:
			raise Exception("Negative pad values not allowed")
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
		if crop_u < 0 or crop_u > self.height or crop_d < crop_u or crop_d > self.height:
			raise Exception("Invalid crop coordinates " + str(crop_u) + ", " + str(crop_d) + " of height " + self.height)
		if pad_u < 0 or pad_d < 0:
			raise Exception("Negative pad values not allowed")
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

def identify_cs16(data: bytes) -> CS16Format:
	"""
	Returns the type of a CS16 header from CS16_FILETYPES, or None.
	"""
	filetype, _ = struct_cs16_header.unpack_from(data, 0)
	if filetype in CS16_FILETYPES:
		return CS16_FILETYPES[filetype]
	return None

def _filetype_or_fail(ft: int) -> CS16Format:
	"""
	Returns the type of a CS16 header from CS16_FILETYPES, or None.
	"""
	if not (ft in CS16_FILETYPES):
		raise Exception("Unknown S16/C16/BLK magic number " + str(ft))
	return CS16_FILETYPES[ft]

def decode_cs16(data: bytes) -> list:
	"""
	Decodes a C16 or S16 file.
	Returns a list of S16Image objects.
	"""
	filetype, count = struct_cs16_header.unpack_from(data, 0)
	cs16fmt = _filetype_or_fail(filetype)
	cfmt = cs16fmt.cfmt
	if cs16fmt.endian != "little":
		raise Exception("No support for non-LE format: " + cs16fmt.desc)
	hptr = 6
	results = []
	for i in range(count):
		iptr, iw, ih = struct_cs16_frame.unpack_from(data, hptr)
		img = S16Image(iw, ih)
		if cs16fmt.compressed:
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
						img.data[pixidx:pixidx + runlen] = _decode_shorts(data[iptr:niptr], cfmt, "little", runlen)
						pixidx += runlen
						iptr = niptr
					else:
						pixidx += runlen
		else:
			hptr += 8
			# just decode everything at once
			iw = iw * ih
			iw2 = iw * ih * 2
			img.data[0:iw] = _decode_shorts(data[iptr:iptr + iw2], cfmt, "little", iw)
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

# ---- RGBA To S16 ----

CDMODE_DEFAULT = "floor"
ADMODE_DEFAULT = "nearest"

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
	cs16fmt = _filetype_or_fail(filetype)
	cfmt = cs16fmt.cfmt
	if cs16fmt.endian != "little":
		raise Exception("No support for non-LE format: " + cs16fmt.desc)
	if cs16fmt.compressed:
		raise Exception("BLK files aren't supposed to be compressed! Found: " + cs16fmt.desc)

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
		img.data[0:iw] = _decode_shorts(data[iptr:iptr + iw2], cfmt, "little", iw)
		iptr += iw2

	return blocks_w, blocks_h, results

def stitch_blk(blocks_w: int, blocks_h: int, blocks: list) -> S16Image:
	"""
	Decodes a BLK file into a full image.
	"""
	block_w, block_h = 128, 128
	if len(blocks) > 0:
		block_w, block_h = blocks[0].width, blocks[0].height
	full = S16Image(blocks_w * block_w, blocks_h * block_h)
	idx = 0
	for x in range(blocks_w):
		for y in range(blocks_h):
			full.blit(blocks[idx], x * block_w, y * block_h, False)
			idx += 1
	return full

def decode_blk(data: bytes) -> S16Image:
	"""
	Decodes a BLK file into a full image.
	"""
	blocks_w, blocks_h, blocks = decode_blk_blocks(data)
	return stitch_blk(blocks_w, blocks_h, blocks)
