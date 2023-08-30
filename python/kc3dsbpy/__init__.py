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
from . import gizmo
from . import imaging

BRAND = bl_info["name"]

from bpy.types import Operator

class RenderKC3DSBPY(Operator):
	# indirectly bound
	bl_idname = "kc3dsbpy.render"
	bl_label = "Render"
	bl_description = "Renders the breed"

	def invoke(self, context, event):
		scene = context.scene
		# setup rendering stuff
		we_own_padding_frame = False
		if not "PaddingFrame" in bpy.data.images:
			padding_frame = bpy.data.images.new("PaddingFrame", 1, 1, alpha = True)
			padding_frame.pixels[0:4] = (0, 0, 0, 0)
			padding_frame.update()
			we_own_padding_frame = True
		else:
			padding_frame = bpy.data.images["PaddingFrame"]
		# actually prepare
		path_base = bpy.path.abspath(scene.render.filepath)
		framereqs = dataext.calc_req_group(scene)
		gizmo_idx = 0
		# Main rendering phase
		for frame in framereqs:
			print("GIZMOBATCH: " + str(gizmo_idx) + " / " + str(len(framereqs)))
			path_png = os.path.join(path_base, frame.paths.png)
			scene.render.image_settings.file_format = "PNG"
			scene.render.image_settings.color_mode = "RGBA"
			out_image = padding_frame
			if type(frame) == dataext.FrameReq:
				frame.activate()
				bpy.ops.render.render()
				frame.deactivate()
				out_image = bpy.data.images["Render Result"]
			imaging.save_image_with_makedirs(out_image, path_png)
			if scene.kc3dsbpy_render_bmp:
				path_bmp = os.path.join(path_base, frame.paths.bmp)
				scene.render.image_settings.file_format = "BMP"
				scene.render.image_settings.color_mode = "RGB"
				tmp_img = bpy.data.images.load(path_png)
				imaging.save_image_with_makedirs(tmp_img, path_bmp)
				bpy.data.images.remove(tmp_img)
			gizmo_idx += 1
		# Done!
		if we_own_padding_frame:
			bpy.data.images.remove(padding_frame)
		self.report({"INFO"}, "Completed render, " + str(gizmo_idx) + " frames handled")
		return {"FINISHED"}

class PNG2C16KC3DSBPY(Operator):
	# indirectly bound
	bl_idname = "kc3dsbpy.png2c16"
	bl_label = "PNG -> C16"
	bl_description = "Converts PNGs to C16s. Uses the rendering configuration for inputs."

	def invoke(self, context, event):
		scene = context.scene
		# actually prepare
		path_ib = bpy.path.abspath(scene.render.filepath)
		path_cb = bpy.path.abspath(scene.kc3dsbpy_c16_outpath)
		framereqs = dataext.calc_req_group(scene)
		# dithering modes
		if scene.kc3dsbpy_c16_dither_colour:
			cdmode = "bayer2"
		else:
			cdmode = "floor"
		if scene.kc3dsbpy_c16_dither_alpha:
			admode = "bayer2"
		else:
			admode = "nearest"
		# actually do the thing
		c16_names = {}
		for frame in framereqs:
			c16_names[frame.paths.c16] = True
		for c16 in c16_names:
			print(c16)
			c16_frames = []
			# load and dither
			for frame in framereqs:
				if frame.paths.c16 != c16:
					continue
				path_png = os.path.join(path_ib, frame.paths.png)
				tmp_img = bpy.data.images.load(path_png)
				c16_frames.append(imaging.bpy_to_s16image(tmp_img, cdmode = cdmode, admode = admode))
				bpy.data.images.remove(tmp_img)
			# finish
			imaging.save_c16_with_makedirs(c16_frames, os.path.join(path_cb, c16))
		self.report({"INFO"}, "Completed PNG->C16, " + str(len(c16_names)) + " files written")
		return {"FINISHED"}

class ActivateFKC3DSBPY(Operator):
	# indirectly bound
	bl_idname = "kc3dsbpy.activate_frame"
	bl_label = "Setup Frame"
	bl_description = "Sets up a specific frame of breed rendering. Useful for troubleshooting."

	def invoke(self, context, event):
		scene = context.scene
		frame = dataext.calc_req_frame(scene)
		if type(frame) == dataext.FrameReq:
			frame.activate()
			self.report({"INFO"}, "Viewing: " + str(frame.paths))
		else:
			self.report({"WARNING"}, "Missing marker '" + frame.part_name + "' @ " + str(frame.paths))
		return {"FINISHED"}

class DeactivateFKC3DSBPY(Operator):
	# indirectly bound
	bl_idname = "kc3dsbpy.deactivate_frame"
	bl_label = "Revert Frame"
	bl_description = "Tries to revert changes caused by Setup Frame."

	def invoke(self, context, event):
		gizmo.GizmoContext(context.scene).deactivate()
		return {"FINISHED"}

# Register/Unregister
def register():
	gizmo.register()
	dataext.register()
	bpy.utils.register_class(RenderKC3DSBPY)
	bpy.utils.register_class(PNG2C16KC3DSBPY)
	bpy.utils.register_class(ActivateFKC3DSBPY)
	bpy.utils.register_class(DeactivateFKC3DSBPY)

def unregister():
	dataext.unregister()
	bpy.utils.unregister_class(RenderKC3DSBPY)
	bpy.utils.unregister_class(PNG2C16KC3DSBPY)
	bpy.utils.unregister_class(ActivateFKC3DSBPY)
	bpy.utils.unregister_class(DeactivateFKC3DSBPY)

if __name__ == "__main__":
	register()

