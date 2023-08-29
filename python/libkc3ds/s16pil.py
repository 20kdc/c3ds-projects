#!/usr/bin/env python3
# S16/C16 library for Python 3, PIL functions
# When copying, it is recommended you write down the commit hash here:
# ________________________________________

# caosprox - CPX server reference implementation
# Written starting in 2022 by contributors (see CREDITS.txt)
# To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
# You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

import PIL.Image
from . import s16
from . import bitdither

def s16image_to_pil_rgba(img: s16.S16Image, alpha_aware: bool = True) -> PIL.Image:
	"""
	Converts a S16Image to a PIL RGBA image.
	If alpha_aware is False, mask colour is totally disrespected.
	"""
	if alpha_aware:
		pil = PIL.Image.new("RGBA", (img.width, img.height))
		pil.putdata(img.to_rgba())
		return pil
	else:
		# awkward, but it works
		pil = PIL.Image.new("RGB", (img.width, img.height))
		pil.putdata(img.to_rgb())
		return pil.convert("RGBA")

def s16image_to_pil_rgb(img: s16.S16Image):
	"""
	Converts a S16Image to a PIL RGB image.
	"""
	pil = PIL.Image.new("RGB", (img.width, img.height))
	pil.putdata(img.to_rgb())
	return pil

def pil_to_565(pil: PIL.Image, false_black: int = s16.COL_BLACK, cdmode: str = s16.CDMODE_DEFAULT, admode: str = s16.ADMODE_DEFAULT) -> s16.S16Image:
	"""
	Encodes a PIL.Image into a 565 S16Image.
	Pixels that would be "accidentally transparent" are nudged to false_black.
	cdmode and admode are dither modes as per the dither function.
	These are for colours and alpha respectively.
	"""
	pil = pil.convert("RGBA")
	return s16.rgba_to_565(pil.width, pil.height, list(pil.getdata(0)), list(pil.getdata(1)), list(pil.getdata(2)), list(pil.getdata(3)), false_black = false_black, cdmode = cdmode, admode = admode)

def pil_to_565_blk(pil: PIL.Image, cdmode: str = "floor") -> s16.S16Image:
	"""
	Encodes a PIL.Image into a 565 S16Image, assuming it will be a BLK file.
	Therefore, alpha and collisions with the masking colour are ignored.
	Note that this doesn't split the image into BLK chunks. encode_blk will do that.
	And this is also useful for non-alpha-aware conversions.
	cdmode is a dither mode as per the dither function.
	"""
	pil = pil.convert("RGB")
	return s16.rgb_to_565_blk(pil.width, pil.height, list(pil.getdata(0)), list(pil.getdata(1)), list(pil.getdata(2)), cdmode = cdmode)

