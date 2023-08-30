#!/usr/bin/env python3
# Images and such

# libkc3ds - General-purpose Python library for C3/DS
# Written starting in 2023 by contributors (see CREDITS.txt)
# To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
# You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

import bpy
from libkc3ds import s16
import os

def save_image_with_makedirs(image, filepath):
	try:
		os.makedirs(os.path.dirname(filepath))
	except:
		pass
	image.save_render(filepath)

def save_c16_with_makedirs(frames, filepath):
	try:
		os.makedirs(os.path.dirname(filepath))
	except:
		pass
	of = open(filepath, "wb")
	of.write(s16.encode_c16(frames))
	of.close()
	
def bpy_to_s16image(image, cdmode: str = s16.CDMODE_DEFAULT, admode: str = s16.ADMODE_DEFAULT) -> s16.S16Image:
	"""
	Converts a Blender Image to an S16Image.
	"""
	w = image.size[0]
	h = image.size[1]
	data_r = [0] * (w * h)
	data_g = [0] * (w * h)
	data_b = [0] * (w * h)
	data_a = [0] * (w * h)
	aind = [data_r, data_g, data_b, data_a]
	tumbler = 0
	for v in image.pixels:
		# ignore value range checks for performance, what could go wrong?
		aind[tumbler].append(int(v * 255))
		tumbler = (tumbler + 1) & 3
	res = s16.rgba_to_565(w, h, data_r, data_g, data_b, data_a, cdmode = cdmode, admode = admode)
	res.flip_y()
	return res


