#!/usr/bin/env python3
# Images and such

# libkc3ds - General-purpose Python library for C3/DS
# Written starting in 2023 by contributors (see CREDITS.txt)
# To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
# You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

import bpy
import imbuf
import os

def save_image_with_makedirs(image, filepath):
	try:
		os.makedirs(os.path.dirname(filepath))
	except:
		pass
	image.save_render(filepath)

def convert_png_to_bmp(srcpath, dstpath):
	pass

def convert_pngs_to_c16(srcpaths, dstpaths):
	pass

