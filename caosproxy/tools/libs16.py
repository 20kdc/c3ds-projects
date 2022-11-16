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

def pil_to_565(pil: PIL.Image, false_black: int = COL_BLACK) -> S16Image:
	"""
	Encodes a PIL.Image into a 565 S16Image.
	Pixels that would be "accidentally transparent" are nudged to false_black.
	"""
	img = S16Image(pil.width, pil.height)
	pil = pil.convert("RGBA")
	idx = 0
	for pixel in pil.getdata():
		r = pixel[0]
		g = pixel[1]
		b = pixel[2]
		a = pixel[3]
		v = 0
		if a >= 128:
			v = ((r << 8) & 0xF800) | ((g << 3) & 0x07E0) | ((b >> 3) & 0x001F)
			if v == 0: # COL_MASK
				v = false_black
		img.data[idx] = v
		idx += 1
	return img

def pil_to_565_blk(pil: PIL.Image) -> S16Image:
	"""
	Encodes a PIL.Image into a 565 S16Image, assuming it will be a BLK file.
	Therefore, alpha and collisions with the masking colour are ignored.
	"""
	img = S16Image(pil.width, pil.height)
	pil = pil.convert("RGB")
	idx = 0
	for pixel in pil.getdata():
		r = pixel[0]
		g = pixel[1]
		b = pixel[2]
		v = ((r << 8) & 0xF800) | ((g << 3) & 0x07E0) | ((b >> 3) & 0x001F)
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
		print("libs16.py encodeS16 <INDIR> <OUT>")
		print(" encodes 565 S16 file from directory")
		print("libs16.py encodeC16 <INDIR> <OUT>")
		print(" encodes 565 C16 file from directory")
		print("libs16.py encodeBLK <IN> <OUT>")
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
		print("")
		print("s16/c16 files are converted to directories of numbered PNG files.")
		print("This process is lossless, though RGB555 files are converted to RGB565.")
		print("The inverse conversion is of course not lossless (lower bits are dropped).")
		print("There is NO dithering.")

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

	def _opt_arg(idx):
		if len(sys.argv) <= idx:
			return None
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

	def encode_fileset(fileset, output, compressed):
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
			imgs.append(pil_to_565(f_pil))
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
		elif sys.argv[1] == "encodeS16":
			encode_fileset(sys.argv[2], sys.argv[3], False)
		elif sys.argv[1] == "encodeC16":
			encode_fileset(sys.argv[2], sys.argv[3], True)
		elif sys.argv[1] == "encodeBLK":
			img = PIL.Image.open(sys.argv[2])
			blk = encode_blk(pil_to_565_blk(img))
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
		else:
			print("cannot understand: " + sys.argv[1])
			command_help()
	else:
		command_help()

