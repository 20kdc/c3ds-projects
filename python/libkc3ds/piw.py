#!/usr/bin/env python3
# Portable Image Writer. Blender doesn't have PIL and writing images is hard in Blender.

# libkc3ds - General-purpose Python library for C3/DS
# Written starting in 2023 by contributors (see CREDITS.txt)
# To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
# You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

import struct
import zlib

def _png_block(name, content):
	ctcd = name + content
	first3 = struct.pack(">I", len(content)) + ctcd
	return first3 + struct.pack(">I", zlib.crc32(ctcd))

def rgba_to_png(w: int, h: int, rgba) -> bytes:
	"""
	Creates a PNG file directly and badly from a list of RGBA tuples.
	"""
	# print("flattening list")
	rgba_flat = []
	for item in rgba:
		rgba_flat += item
	# print("producing content")
	idat_content = b""
	for row in range(h):
		idx = w * row * 4
		idx2 = idx + (w * 4)
		idat_content += b"\x00"
		idat_content += bytes(rgba_flat[idx:idx2])
	idat_content = zlib.compress(idat_content)
	ihdr_content = struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0)
	return b"\x89\x50\x4E\x47\x0D\x0A\x1A\x0A" + _png_block(b"IHDR", ihdr_content) + _png_block(b"IDAT", idat_content) + _png_block(b"IEND", b"")

