#!/usr/bin/env python3
# Blender addon?

# caosprox - CPX server reference implementation
# Written starting in 2022 by contributors (see CREDITS.txt)
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
import imbuf
import os
from . import dataext
from . import gizmo
from . import database
from . import chichi

BRAND = bl_info["name"]

from bpy_extras.io_utils import ExportHelper
from bpy.props import StringProperty, IntProperty, FloatProperty, BoolProperty, PointerProperty
from bpy.types import Operator, Panel

def debug_activate(idx):
	"""
	Debug the state of a specific Gizmo activation.
	"""
	frames = []
	chichi.CHICHI.gen_frames(frames, {"filepath": ""})
	gizmos = []
	for v in frames:
		if not (v is None):
			gizmos.append(gizmo.Gizmo(bpy.context, v))
	gizmos[idx].activate()

def save_image_with_makedirs(image, filepath):
	try:
		os.makedirs(os.path.dirname(filepath))
	except:
		pass
	image.save_render(filepath)

def render_gizmo_frames(context, path_base, frames):
	# initial setup
	context.scene.render.image_settings.file_format = "BMP"
	context.scene.render.image_settings.color_mode = "RGB"
	if not "PaddingFrame" in bpy.data.images:
		padding_frame = bpy.data.images.new("PaddingFrame", 1, 1)
	else:
		padding_frame = bpy.data.images["PaddingFrame"]
	# continue...
	gizmos = []
	for v in frames:
		if v["real"]:
			gizmos.append(gizmo.Gizmo(context, v))
		else:
			save_image_with_makedirs(padding_frame, v["filepath"])
	gizmo_idx = 0
	for gz in gizmos:
		print("GIZMOBATCH: " + str(gizmo_idx) + " / " + str(len(gizmos)))
		gz.activate()
		try:
			bpy.ops.render.render(context)
			save_image_with_makedirs(bpy.data.images["Render Result"], gz.props["filepath"])
		finally:
			gz.deactivate()
		gizmo_idx += 1

class DoTheThingKC3DSBPY(Operator):
	bl_idname = "kc3dsbpy.dothething"
	bl_label = BRAND + ": Do The Thing"
	bl_description = "Renders out a creature to the render output directory. Assumes everything is already perfectly configured."

	def invoke(self, context, event):
		path_base = os.path.join(bpy.path.abspath(context.scene.render.filepath), "Norn", "z")
		frames = []
		chichi.CHICHI.gen_frames(frames, {"filepath": path_base})
		render_gizmo_frames(context, path_base, frames)
		return {"FINISHED"}

class OneSkeletonKC3DSBPY(Operator):
	bl_idname = "kc3dsbpy.oneskeleton"
	bl_label = BRAND + ": One Skeleton Only"
	bl_description = "Renders out one creature skeleton to the render output directory. Assumes everything is already perfectly configured."

	def invoke(self, context, event):
		path_base = os.path.join(bpy.path.abspath(context.scene.render.filepath), "Norn", "z")
		frames = []
		# TODO: Actually allow configuring any of this
		chichi.CHICHI.ages[0].gen_frames(frames, {"filepath": path_base, "male": 1, "female": 0})
		render_gizmo_frames(context, path_base, frames)
		return {"FINISHED"}

def menu_render(self, context):
	self.layout.operator(DoTheThingKC3DSBPY.bl_idname)
	self.layout.operator(OneSkeletonKC3DSBPY.bl_idname)

# Register/Unregister
def register():
	dataext.register()
	# UI
	bpy.utils.register_class(DoTheThingKC3DSBPY)
	bpy.utils.register_class(OneSkeletonKC3DSBPY)
	bpy.types.TOPBAR_MT_render.append(menu_render)

def unregister():
	dataext.unregister()
	# UI
	bpy.utils.unregister_class(DoTheThingKC3DSBPY)
	bpy.utils.unregister_class(OneSkeletonKC3DSBPY)
	bpy.types.TOPBAR_MT_render.remove(menu_render)

if __name__ == "__main__":
	register()

