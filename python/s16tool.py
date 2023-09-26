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
import random
import sys

import PIL.Image

from libkc3ds.s16 import decode_cs16, decode_blk, encode_c16, encode_s16, identify_cs16, stitch_blk, S16Image, CDMODE_DEFAULT, ADMODE_DEFAULT
from libkc3ds.s16pil import pil_to_565, pil_to_565_blk, s16image_to_pil_rgba
from libkc3ds.bitdither import dither_channel

# ---- Command Line ----

def command_help():
	print("s16tool.py info <IN...>")
	print(" information on c16/s16 files")
	print("s16tool.py infoShort <IN...>")
	print(" short information on c16/s16 files")
	print("s16tool.py encodeS16 <INDIR> <OUT> [<CDMODE> [<ADMODE>]]")
	print(" encodes 565 S16 file from directory")
	print("s16tool.py encodeC16 <INDIR> <OUT> [<CDMODE> [<ADMODE>]]")
	print(" encodes 565 C16 file from directory")
	print("s16tool.py encodeBLK <IN> <OUT> [<CDMODE>]")
	print(" encodes 565 BLK file from source")
	print("s16tool.py decode <IN> <OUTDIR>")
	print(" decodes S16 or C16 files")
	print("s16tool.py decodeFrame <IN> <FRAME> <OUT>")
	print(" decodes a single frame of a S16/C16 to a PNG file")
	print("s16tool.py decodeFrameBMP <IN> <FRAME> <OUT>")
	print(" decodes a single frame of a S16/C16 to a BMP file")
	print("s16tool.py decodeBLK <IN> <OUT>")
	print(" decodes a BLK file to a PNG file")
	print("s16tool.py decodeC2B <IN> <OUT>")
	print(" decodes a C2 background file to a PNG file")
	print("s16tool.py mask <SOURCE> <X> <Y> <VICTIM> <FRAME> [<CHECKPRE> [<CHECKPOST>]]")
	print(" **REWRITES** the given FRAME of the VICTIM file to use the colours from SOURCE frame 0 at the given X/Y position, but basing alpha on the existing data in the frame.")
	print(" CHECKPRE/CHECKPOST are useful for comparisons.")
	print("s16tool.py shift <VICTIM> <FRAME> <X> <Y> [<CHECKPRE> [<CHECKPOST>]]")
	print(" **REWRITES** the given FRAME of the VICTIM file to shift it by X and Y.")
	print(" CHECKPRE/CHECKPOST are useful for comparisons.")
	print(" If the X and Y values are *negative*, pixels are lost. If they are *positive*, transparent pixels are added.")
	print("s16tool.py blit <SOURCE> <SRCFRAME> <VICTIM> <DSTFRAME> <X> <Y> [<CHECKPRE> [<CHECKPOST>]]")
	print(" **REWRITES** the given DSTFRAME of the VICTIM file, blitting SOURCE's SRCFRAME to it.")
	print(" This blit is alpha-aware.")
	print(" CHECKPRE/CHECKPOST are useful for comparisons.")
	print("s16tool.py genPalRef <DST>")
	print(" generates a PNG palette reference file")
	print("s16tool.py dither <IN> <OUT> [<CDMODE> [<ADMODE> [<RBITS> [<GBITS> [<BBITS> [<ABITS>]]]]]]")
	print(" tests dithering - defaults to RGB565")
	print("s16tool.py dithera <IN> [<CDMODE> [<ADMODE> [<RBITS> [<GBITS> [<BBITS> [<ABITS>]]]]]]")
	print(" dither command, but output name is inferred from details")
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
	print(" The following are considered 'pattern sets'.")
	print(" They are all internally ordered dithers that all have a '-random' variant.")
	print(" This variant interpolates between the stages using randomness.")
	print(" checkers: 2x2 checkerboard in the 25% to 75% range.")
	print("           Adds less than a bit of \"effective depth\", but very reliable.")
	print(" always-checkers: Checkerboard whenever the value is not exactly equal.")
	print(" bayer2: Bayer 2x2 as described by https://github.com/SixLabors/ImageSharp/blob/main/src/ImageSharp/Processing/Processors/Dithering/DHALF.TXT")
	print(" bayer4: Bayer 4x4 from same")
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
			s16image_to_pil_rgba(fr).save(fn, "PNG")

def _opt_arg(idx, df = None):
	if len(sys.argv) <= idx:
		return df
	return sys.argv[idx]

def _write_equal_format(fn, images, original):
	f = open(fn, "wb")
	if identify_cs16(original).compressed:
		f.write(encode_c16(images))
	else:
		f.write(encode_s16(images))
	f.close()

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
		for fn in sys.argv[2:]:
			data = _read_bytes(fn)
			info = identify_cs16(data)
			if info == None:
				print(fn + ": Unknown!")
			else:
				print(fn + ": " + info.desc)
			# this COULD fail (no support for Mac files), do it late
			images = decode_cs16(data)
			idx = 0
			print(str(len(images)) + " frames")
			for v in images:
				print(" " + str(idx) + ": " + str(v.width) + "x" + str(v.height))
				idx += 1
	elif sys.argv[1] == "infoShort":
		for fn in sys.argv[2:]:
			data = _read_bytes(fn)
			info = identify_cs16(data)
			if info == None:
				print(fn + ": Unknown!")
			else:
				try:
					images = decode_cs16(data)
					print(fn + ": " + info.desc + ", " + str(len(images)) + " frames")
				except:
					print(info.desc)
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
			vpil = s16image_to_pil_rgba(v)
			vpil.save(os.path.join(sys.argv[3], str(idx) + ".png"), "PNG")
			idx += 1
	elif sys.argv[1] == "decodeFrame":
		frame = int(sys.argv[3])
		images = _read_cs16_file(sys.argv[2])
		vpil = s16image_to_pil_rgba(images[frame])
		vpil.save(sys.argv[4], "PNG")
	elif sys.argv[1] == "decodeFrameBMP":
		frame = int(sys.argv[3])
		images = _read_cs16_file(sys.argv[2])
		bmp = images[frame].to_bmp()
		f = open(sys.argv[4], "wb")
		f.write(bmp)
		f.close()
	elif sys.argv[1] == "decodeBLK":
		blk = decode_blk(_read_bytes(sys.argv[2]))
		vpil = s16image_to_pil_rgba(blk, alpha_aware = False)
		vpil.save(sys.argv[3], "PNG")
	elif sys.argv[1] == "decodeC2B":
		images = _read_cs16_file(sys.argv[2])
		vpil = s16image_to_pil_rgba(stitch_blk(len(images) // 16, 16, images), alpha_aware = False)
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
		vpil = s16image_to_pil_rgba(image, alpha_aware = False)
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
		bits = [rbits, gbits, bbits, abits]
		# autogen name
		if fno == None:
			fno = os.path.join(os.path.dirname(fni), os.path.basename(fni) + "." + cdmode + str(rbits) + str(gbits) + str(bbits) + "." + admode + str(abits) + ".png")
		# alright, load
		vpil = PIL.Image.open(fni)
		vpil = vpil.convert("RGBA")
		# convert
		data = [list(vpil.getdata(i)) for i in range(4)]
		# dither
		for i in range(3):
			dither_channel(vpil.width, vpil.height, data[i], bits[i], cdmode)
		dither_channel(vpil.width, vpil.height, data[3], bits[3], admode)
		# convert
		data_total = []
		for i in range(vpil.width * vpil.height):
			data_total.append(tuple([BITCOPY_TABLES[bits[j]][data[j][i]] for j in range(4)]))
		vpil.putdata(data_total)
		# done
		vpil.save(fno, "PNG")
	else:
		print("cannot understand: " + sys.argv[1])
		command_help()
else:
	command_help()
