#!/usr/bin/env python3
# Creatures 3 Breed Sizes

# caosprox - CPX server reference implementation
# Written starting in 2022 by contributors (see CREDITS.txt)
# To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
# You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

import argparse
from libkc3ds.s16 import decode_cs16, S16Image
from libkc3ds.parts import C3_0, C3_GS_MAP, SETUP
import sys
import os

# Constants

GENUSES = ["Norn", "Grendel", "Ettin", "Geat"]
BREEDS = []
for i in range(26):
	BREEDS.append(chr(97 + i))
SEXES = ["male", "female"]
AGES = ["0", "1", "2", "3", "4", "5", "6"]

SETUPS = []
for setup in SETUP:
	SETUPS.append(setup)

DESCRIPTION = """
Reports the sizes of the first frame of each part in each skeleton found.
"""

sprite_cache_path = None
sprite_cache_dec = None

def spr_path(base, genus, breed_char, sex, age_char, part_char, ext):
	return os.path.join(base, part_char + C3_GS_MAP[sex + genus] + age_char + breed_char + "." + ext)

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

parser = argparse.ArgumentParser(prog="c3breedsizes.py", description=DESCRIPTION)
parser.add_argument("IMAGES", help="S16/C16 files are loaded here.")
parser.add_argument("INFMT", help="Conversion direction; this is the input file type. Careful not to get this wrong, or you'll overwrite all your files!", choices=["c16", "s16"])
parser.add_argument("-g", "--genus", help="Limits to a specific genus.", choices=GENUSES, action="append")
parser.add_argument("-b", "--breed", help="Limits to a specific breed slot.", choices=BREEDS, action="append")
parser.add_argument("-s", "--sex", help="Limits to a specific sex.", choices=SEXES, action="append")
parser.add_argument("-a", "--age", help="Limits to a specific age.", choices=AGES, action="append")
parser.add_argument("-e", "--engine", "--game", "--setup", help="Selects the 'setup' (game skeleton configuration).", choices=SETUPS, default="c3")
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

# main iteration

did_anything = False

setup = SETUP[args.engine]

engine_caps = args.engine.upper()

print("EXAMPLE = CSet(\"EXAMPLE\", \"EXAMPLE\", " + engine_caps + ", [")
for genus in args.genus:
	for breed in args.breed:
		for sex in args.sex:
			for age in args.age:
				sprites = args.IMAGES
				any_part_exists = False
				for part_info in setup.part_infos:
					if os.path.exists(spr_path(sprites, genus, breed, sex, age, part_info.char, args.INFMT)):
						any_part_exists = True
						break
				if not any_part_exists:
					continue
				print("\tCAge(\"" + age + "\", XXX, [")
				for part_info in setup.part_infos:
					spr_in = spr_path(sprites, genus, breed, sex, age, part_info.char, args.INFMT)
					frame = read_spr(spr_in, 0, 1, 1)
					if frame.width != frame.height:
						print("\t\t# WARNING: " + str(frame.width) + "x" + str(frame.height))
					print("\t\tAgedPart(" + engine_caps + "_" + part_info.char + ", " + str(frame.width) + "),")
					did_anything = True
				print("\t]),")
print("])")

if not did_anything:
	print("Nothing was performed! Did you configure your paths correctly?")

