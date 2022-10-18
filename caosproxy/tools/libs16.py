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

struct_cs16_header = struct.Struct("<IH")
struct_cs16_frame = struct.Struct("<IHH")
struct_cs16_pixel = struct.Struct("<H")
struct_cs16_lofs = struct.Struct("<I")

class S16Image():
	"""
	16-bit image that can either be in 555 or 565 format.
	"""
	def __init__(self, w: int, h: int, is_555: bool, data = None):
		self.width = w
		self.height = h
		self.is_555 = is_555
		if data == None:
			self.data = [0] * (w * h)
		else:
			self.data = data

	def copy(self):
		"""
		Creates a copy of this image.
		"""
		return S16Image(self.width, self.height, self.is_555, self.data.copy())

	def convert_565(self):
		"""
		Converts this image in-place to 565 format.
		"""
		if not self.is_555:
			# no need
			return
		for i in range(self.width * self.height):
			v = self.data[i]
			# cheeky!
			#      ----____----____
			# 555: 0RRRRRGGGGGBBBBB
			# 565: RRRRRGGGGGgBBBBB
			# first moves R/G source fields up 1 to match 565
			# second copies upper G bit
			# third handles B
			v = ((v & 0x7FE0) << 1) | ((v & 0x0200) >> 4) | (v & 0x001F)
			self.data[i] = v
		self.is_555 = False

	def to_pil(self):
		"""
		Converts the image to a PIL image.
		"""
		target = self
		if self.is_555:
			target = self.copy()
			target.convert_565()
		pixseq = [(0, 0, 0, 0)] * (target.width * target.height)
		for i in range(target.width * target.height):
			v = target.data[i]
			if v != 0:
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
		img = S16Image(iw, ih, is_555)
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
						for vt in struct_cs16_pixel.iter_unpack(data[iptr:niptr]):
							img.data[pixidx] = vt[0]
							pixidx += 1
						iptr = niptr
					else:
						pixidx += runlen
		else:
			hptr += 8
			# just decode everything at once
			iw2 = iw * ih * 2
			pixidx = 0
			for vt in struct_cs16_pixel.iter_unpack(data[iptr:iptr + iw2]):
				img.data[pixidx] = vt[0]
				pixidx += 1
		results.append(img)
	return results

def encode_s16(images) -> bytes:
	"""
	Encodes a S16 file from S16Image objects.
	Returns bytes.
	"""
	data = struct_cs16_header.pack(1, len(images))
	blob_ptr = struct_cs16_header.size + (struct_cs16_frame.size * len(images))
	blob = b""
	for v in images:
		data += struct_cs16_frame.pack(blob_ptr, v.width, v.height)
		for xv in v.data:
			blob += struct_cs16_pixel.pack(xv)
		blob_ptr += len(v.data) * 2
	return data + blob

def _encode_c16_run(line_p, run):
	if len(run) == 0:
		return
	if run[0] == 0:
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
		vtr = v == 0
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
	res = b""
	for v in line_p:
		res += struct_cs16_pixel.pack(v)
	return res

def encode_c16(images) -> bytes:
	"""
	Encodes a C16 file from S16Image objects.
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

def pil_to_565(pil: PIL.Image, false_black: int = 0x0020) -> S16Image:
	"""
	Encodes a PIL.Image into a 565 S16Image.
	Pixels that would be "accidentally transparent" are nudged to false_black.
	"""
	img = S16Image(pil.width, pil.height, False)
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
			if v == 0:
				v = false_black
		img.data[idx] = v
		idx += 1
	return img

if __name__ == "__main__":

	def command_help():
		print("libs16.py encodeS16 <INDIR> <OUT>")
		print(" encodes 565 s16 file from directory")
		print("libs16.py encodeC16 <INDIR> <OUT>")
		print(" encodes 565 c16 file from directory")
		print("libs16.py decode <IN> <OUTDIR>")
		print(" decodes s16 or c16 files")
		print("")
		print(" s16/c16 files are converted to directories of numbered PNG files.")
		print(" This process is lossless, with a potential oddity if the files are in 555 format rather than 565.")
		print(" The inverse conversion is of course not lossless (lower bits are dropped).")
		print(" There is NO dithering.")

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
			imgs.append(pil_to_565(PIL.Image.open(f_path)))
			idx += 1
		if compressed:
			data = encode_c16(imgs)
		else:
			data = encode_s16(imgs)
		res = open(output, "wb")
		res.write(data)
		res.close()

	if len(sys.argv) >= 2:
		if sys.argv[1] == "encodeS16":
			encode_fileset(sys.argv[2], sys.argv[3], False)
		elif sys.argv[1] == "encodeC16":
			encode_fileset(sys.argv[2], sys.argv[3], True)
		elif sys.argv[1] == "decode":
			try:
				os.mkdir(sys.argv[3])
			except:
				# we don't care
				pass
			f = open(sys.argv[2], "rb")
			images = decode_cs16(f.read())
			idx = 0
			for v in images:
				vpil = v.to_pil()
				vpil.save(os.path.join(sys.argv[3], str(idx) + ".png"), "PNG")
				idx += 1
			f.close()
		else:
			print("cannot understand: " + sys.argv[1])
			command_help()
	else:
		command_help()

