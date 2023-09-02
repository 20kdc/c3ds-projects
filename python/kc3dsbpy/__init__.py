#!/usr/bin/env python3
# Blender addon?

# libkc3ds - General-purpose Python library for C3/DS
# Written starting in 2023 by contributors (see CREDITS.txt)
# To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
# You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

bl_info = {
	"name": "C3/DS Breed Exporter",
	"version": (0, 1, 0),
	"blender": (3, 6, 0),
	"category": "Import-Export",
	"location": "File > Import/Export",
	"description": "C3/DS breed exporter. Works via magic."
}

import bpy
import os
from libkc3ds import s16
from . import dataext
from . import framereq
from . import gizmo

BRAND = bl_info["name"]

# Register/Unregister
def register():
	gizmo.register()
	dataext.register()
	framereq.register()

def unregister():
	dataext.unregister()
	gizmo.unregister()
	framereq.unregister()

if __name__ == "__main__":
	register()

