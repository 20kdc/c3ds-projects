#!/usr/bin/env python3
# Creatures 3 Breed BMP Tree Exporter/Importer

# caosprox - CPX server reference implementation
# Written starting in 2022 by contributors (see CREDITS.txt)
# To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
# You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

import argparse
import PIL.Image
from libkc3ds.s16 import decode_cs16, S16Image, CDMODE_DEFAULT, encode_c16
from libkc3ds.s16pil import pil_to_565_blk, s16image_to_pil_rgb
from libkc3ds.parts import C3, C3_0, C3_GS_MAP
import sys
import os

# Constants

GENUSES = ["Norn", "Grendel", "Ettin", "Geat"]
BREEDS = []
for i in range(26):
	BREEDS.append(chr(97 + i))
SEXES = ["male", "female"]
AGES = ["0", "1", "2", "3", "4", "5", "6"]

DESCRIPTION = """
Imports and exports breed BMP trees in the QuickNorn/SpriteBuilder convention.
All missing frames are replaced with 1x1 blanks to avoid crashes.
When converting to C16, 
"""

# Image IO

def bmp_path(base, frame):
	fs = str(frame)
	while len(fs) < 4:
		fs = "0" + fs
	return os.path.join(base, "CA" + fs + ".bmp")

def read_bmp(base, frame, w, h):
	try:
		return PIL.Image.open(bmp_path(base, frame))
	except:
		return PIL.Image.new("RGB", (w, h))

sprite_cache_path = None
sprite_cache_dec = None

def spr_path(base, genus, breed_char, sex, age_char, part_char):
	return os.path.join(base, part_char + C3_GS_MAP[sex + genus] + age_char + breed_char + ".c16")

def load_spr(path):
	global sprite_cache_path, sprite_cache_dec
	if path == sprite_cache_path:
		return sprite_cache_dec
	sprite_cache_path = path
	try:
		f = open(path, "rb")
		data = f.read()
		f.close()
		sprite_cache_dec = decode_cs16(data)
	except Exception as ex:
		print(ex)
		sprite_cache_dec = []
	return sprite_cache_dec

def read_spr(path, frame, w, h):
	frames = load_spr(path)
	if frame < 0 or frame >= len(frames):
		return S16Image(w, h)
	return frames[frame]

# Command definition

parser = argparse.ArgumentParser(prog="c3breedie.py", description=DESCRIPTION)
parser.add_argument("IMAGES", help="S16/C16 files are stored/loaded here.")
parser.add_argument("TREE", help="Root of the tree. For, say, '/home/grendelhugger/Grendel/z/male/4/CAxxxx.bmp', this would be '/home/grendelhugger/'.")
parser.add_argument("OPERATOR", help="Conversion direction; this is the output file type. Careful not to get this wrong, or you'll overwrite all your files!", choices=["c16", "bmp"])
parser.add_argument("-g", "--genus", help="Limits to a specific genus.", choices=GENUSES, action="append")
parser.add_argument("-b", "--breed", help="Limits to a specific breed slot.", choices=BREEDS, action="append")
parser.add_argument("-s", "--sex", help="Limits to a specific sex.", choices=SEXES, action="append")
parser.add_argument("-a", "--age", help="Limits to a specific age.", choices=AGES, action="append")
parser.add_argument("--c16-include-0", action="store_true", help="Includes part 0 files for C16 operator. (BMP operator always includes them.)")
parser.add_argument("-m", "--cdmode", help="Colour dither mode as per s16tool.py (defaults to " + CDMODE_DEFAULT + ")", default=CDMODE_DEFAULT)
args = parser.parse_args()

# setup filters
if args.genus is None:
	args.genus = GENUSES
if args.breed is None:
	args.breed = BREEDS
if args.sex is None:
	args.sex = SEXES
if args.age is None:
	args.age = AGES

# main operator

def bmp_single_sprite_file(spr, tree, genus, breed, sex, age, frame_base, frame_count):
	for i in range(frame_count):
		frame = read_spr(spr, i, 1, 1)
		frame_pil = s16image_to_pil_rgb(frame)
		frame_pil.save(bmp_path(tree, frame_base + i), "BMP")

def c16_single_sprite_file(spr, tree, genus, breed, sex, age, frame_base, frame_count):
	frames = []
	for i in range(frame_count):
		bmp = read_bmp(tree, frame_base + i, 1, 1)
		frames.append(pil_to_565_blk(bmp, cdmode = args.cdmode))
	f = open(spr, "wb")
	f.write(encode_c16(frames))
	f.close()

# main iteration

did_anything = False

for genus in args.genus:
	for breed in args.breed:
		for sex in args.sex:
			for age in args.age:
				sprites = args.IMAGES
				tree = os.path.join(args.TREE, genus, breed, sex, age)
				if args.OPERATOR == "bmp":
					any_part_exists = False
					for part_info in C3.part_infos:
						if os.path.exists(spr_path(sprites, genus, breed, sex, age, part_info.char)):
							any_part_exists = True
							break
					if not any_part_exists:
						print("Skipping " + tree + " as no sprite files exist.")
						continue
					try:
						os.makedirs(tree)
					except:
						pass
				if args.OPERATOR == "c16":
					if not os.path.exists(tree):
						print("Skipping " + tree + " as the base directory doesn't exist.")
						continue
					try:
						os.makedirs(sprites)
					except:
						pass
				for part_info in C3.part_infos:
					spr = spr_path(sprites, genus, breed, sex, age, part_info.char)
					if args.OPERATOR == "bmp":
						# we don't want to skip parts of this because QuickNorn gets salty when images are missing
						print(spr + " -> " + bmp_path(tree, part_info.frame_base) + " (...)")
						bmp_single_sprite_file(spr, tree, genus, breed, sex, age, part_info.frame_base, len(part_info.frames))
					elif args.OPERATOR == "c16":
						if part_info.part_id == C3_0:
							if not args.c16_include_0:
								continue
						print(bmp_path(tree, part_info.frame_base) + " (...) -> " + spr)
						c16_single_sprite_file(spr, tree, genus, breed, sex, age, part_info.frame_base, len(part_info.frames))
					else:
						raise Exception("Unknown operator " + args.OPERATOR)
					did_anything = True

if not did_anything:
	print("Nothing was performed! Did you configure your paths correctly?")

