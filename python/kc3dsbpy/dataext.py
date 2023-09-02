#!/usr/bin/env python3
# Blender addon data, frame request calculator

# libkc3ds - General-purpose Python library for C3/DS
# Written starting in 2023 by contributors (see CREDITS.txt)
# To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
# You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

import os
import webbrowser

import bpy
from bpy.props import StringProperty, EnumProperty, IntProperty, FloatProperty, BoolProperty, PointerProperty
from bpy.types import Operator, Panel

import libkc3ds.parts

from . import gizmo
from . import database
from . import framereq

# need to import this here, bleh
# still better than polluting the mess that is __init__
BRAND = "C3/DS Breed Exporter"

CSETS_ITEM_LIST = []

for cset in database.CSETS_ALL:
	CSETS_ITEM_LIST.append((cset.name, cset.desc, "Template: " + cset.desc))

# Data UI

class ObjectHelpKC3DSBPY(Operator):
	bl_idname = "kc3dsbpy.objecthelp"
	bl_label = "Help"
	bl_description = "View help for kc3dsbpy"

	def invoke(self, context, event):
		webbrowser.open("https://github.com/20kdc/c3ds-projects/blob/main/python/kc3dsbpy/HELP.md")
		return {"FINISHED"}

class CouplePartToVisKC3DSBPY(Operator):
	bl_idname = "kc3dsbpy.coupleparttovis"
	bl_label = "Into VisScript"
	bl_description = "Sets the VisScript so this Object is only visible if the current part is that set as the Marker"

	def invoke(self, context, event):
		context.object.kc3dsbpy_visscript = "part=" + context.object.kc3dsbpy_part_marker
		return {"FINISHED"}

class PitchAutomaticToManualKC3DSBPY(Operator):
	bl_idname = "kc3dsbpy.pitchautomatictomanual"
	bl_label = "Convert To Manual"
	bl_description = "Converts automatic pitch to manual pitch"

	def invoke(self, context, event):
		context.object.kc3dsbpy_pitch_manual = True
		context.object.kc3dsbpy_pitch_fm1 = framereq.calc_pitch_auto(context.object, -1)
		context.object.kc3dsbpy_pitch_f0 = framereq.calc_pitch_auto(context.object, 0)
		context.object.kc3dsbpy_pitch_f1 = framereq.calc_pitch_auto(context.object, 1)
		context.object.kc3dsbpy_pitch_f2 = framereq.calc_pitch_auto(context.object, 2)
		context.object.kc3dsbpy_pitch_sm1 = context.object.kc3dsbpy_pitch_fm1
		context.object.kc3dsbpy_pitch_s0 = context.object.kc3dsbpy_pitch_f0
		context.object.kc3dsbpy_pitch_s1 = context.object.kc3dsbpy_pitch_f1
		context.object.kc3dsbpy_pitch_s2 = context.object.kc3dsbpy_pitch_f2
		return {"FINISHED"}

class PitchManualToAutomaticKC3DSBPY(Operator):
	bl_idname = "kc3dsbpy.pitchmanualtoautomatic"
	bl_label = "Convert To Automatic"
	bl_description = "Converts manual pitch to automatic pitch, based on FM1 and F2"

	def invoke(self, context, event):
		context.object.kc3dsbpy_pitch_manual = False
		context.object.kc3dsbpy_pitch_mul = framereq.calc_pitch_mul(context.object.kc3dsbpy_pitch_fm1, context.object.kc3dsbpy_pitch_f2)
		context.object.kc3dsbpy_pitch_trim = framereq.calc_pitch_trim(context.object.kc3dsbpy_pitch_fm1, context.object.kc3dsbpy_pitch_f2)
		return {"FINISHED"}

class FrameRelativeSeekKC3DSBPY(Operator):
	bl_idname = "kc3dsbpy.frame_relative_seek"
	bl_label = "Frame Relative Seek"
	bl_description = "Seeks between frames and auto-activates the frame."

	adjustment: IntProperty(name="Adjustment", description="Adjustment in frames.", default=16)

	def invoke(self, context, event):
		context.scene.kc3dsbpy_render_frame += self.adjustment
		framereq.activate_frame_op(self, context.scene)
		return {"FINISHED"}

def calc_frame_status(scene):
	try:
		frame_idx = scene.kc3dsbpy_render_frame
		cset = framereq.scene_to_cset(scene)
		frame_set = cset.setup.frames
		frame_status = str(frame_idx) + "/" + str(len(frame_set))
		if frame_idx < 0 or frame_idx >= len(frame_set):
			frame_status += " out of range"
		else:
			frame_props = frame_set[frame_idx]
			part_name = frame_props["part"]
			part_char = cset.setup.part_names_to_infos[part_name].char
			frame_status += "=" + part_name + "(" + part_char + ")." + str(frame_props["frame_rel"])
		return frame_status
	except:
		return "(unknown)"

class SCENE_PT_ScenePanelKC3DSBPY(Panel):
	bl_space_type = "PROPERTIES"
	bl_region_type = "WINDOW"
	bl_context = "scene"
	bl_label = BRAND
	def draw(self, context):
		self.layout.prop(context.scene, "kc3dsbpy_cset")
		row = self.layout.row()
		row.prop(context.scene, "kc3dsbpy_render_genus")
		row.prop(context.scene, "kc3dsbpy_render_breed")
		row = self.layout.row()
		row.prop(context.scene, "kc3dsbpy_render_sexes")
		row.prop(context.scene, "kc3dsbpy_render_ages")
		self.layout.prop(context.scene, "kc3dsbpy_render_ppu")
		self.layout.prop(context.scene, "kc3dsbpy_render_bmp")
		row = self.layout.row()
		row.operator("kc3dsbpy.render")
		row.prop(context.scene, "kc3dsbpy_render_mode")
		self.layout.separator()
		row = self.layout.row()
		row.prop(context.scene, "kc3dsbpy_c16_dither_colour")
		row.prop(context.scene, "kc3dsbpy_c16_dither_alpha")
		self.layout.prop(context.scene, "kc3dsbpy_c16_outpath")
		self.layout.operator("kc3dsbpy.png2c16")
		self.layout.separator()
		row = self.layout.row()
		row.prop(context.scene, "kc3dsbpy_render_sex")
		row.prop(context.scene, "kc3dsbpy_render_age")
		row = self.layout.row()
		row.prop(context.scene, "kc3dsbpy_render_frame")
		row.label(text = calc_frame_status(context.scene))
		row = self.layout.row()
		opp = row.operator(FrameRelativeSeekKC3DSBPY.bl_idname, text="-16")
		opp["adjustment"] = -16
		opp = row.operator(FrameRelativeSeekKC3DSBPY.bl_idname, text="-1")
		opp["adjustment"] = -1
		row.operator("kc3dsbpy.activate_frame", text="0")
		opp = row.operator(FrameRelativeSeekKC3DSBPY.bl_idname, text="+1")
		opp["adjustment"] = 1
		opp = row.operator(FrameRelativeSeekKC3DSBPY.bl_idname, text="+16")
		opp["adjustment"] = 16
		row.operator("kc3dsbpy.deactivate_frame", text="Revert")
		self.layout.operator(ObjectHelpKC3DSBPY.bl_idname)

class OBJECT_PT_ObjectPanelKC3DSBPY(Panel):
	bl_space_type = "PROPERTIES"
	bl_region_type = "WINDOW"
	bl_context = "object"
	bl_label = BRAND
	def draw(self, context):
		row = self.layout.row()
		row.prop(context.object, "kc3dsbpy_part_marker")
		row.operator(CouplePartToVisKC3DSBPY.bl_idname)
		if context.object.kc3dsbpy_part_marker != "0":
			self.layout.prop(context.object, "kc3dsbpy_marker_inherit")
			if context.object.kc3dsbpy_marker_inherit is None:
				if context.object.kc3dsbpy_pitch_manual:
					row = self.layout.row()
					row.prop(context.object, "kc3dsbpy_pitch_manual")
					row.operator(PitchManualToAutomaticKC3DSBPY.bl_idname)
					row = self.layout.row()
					row.prop(context.object, "kc3dsbpy_pitch_fm1")
					row.prop(context.object, "kc3dsbpy_pitch_sm1")
					row = self.layout.row()
					row.prop(context.object, "kc3dsbpy_pitch_f0")
					row.prop(context.object, "kc3dsbpy_pitch_s0")
					row = self.layout.row()
					row.prop(context.object, "kc3dsbpy_pitch_f1")
					row.prop(context.object, "kc3dsbpy_pitch_s1")
					row = self.layout.row()
					row.prop(context.object, "kc3dsbpy_pitch_f2")
					row.prop(context.object, "kc3dsbpy_pitch_s2")
				else:
					row = self.layout.row()
					row.prop(context.object, "kc3dsbpy_pitch_manual")
					row.operator(PitchAutomaticToManualKC3DSBPY.bl_idname)
					row = self.layout.row()
					row.prop(context.object, "kc3dsbpy_pitch_mul")
					row.prop(context.object, "kc3dsbpy_pitch_trim")
				self.layout.prop(context.object, "kc3dsbpy_ppu_factor")
		self.layout.prop(context.object, "kc3dsbpy_visscript")
		self.layout.operator(ObjectHelpKC3DSBPY.bl_idname)

# Registration and stuff

def register():
	# Part IDs
	all_part_ids = []
	all_part_ids.append(("0", "(None)", "Disabled"))
	for name in libkc3ds.parts.ALL:
		all_part_ids.append((name, name, "Used as camera location for part: " + name))
	# Kind of shared with Gizmo but will just have to live with it due to the items
	bpy.types.Object.kc3dsbpy_part_marker = EnumProperty(items = all_part_ids, name = "Marker", default = "0",
	description = "If set, the camera uses this object as the reference point for rendering images of the given part. In addition, this object will be rotated for pitch/yaw, and this object acts as the per-part settings. There can only be one of these for each part in the scene")
	bpy.types.Object.kc3dsbpy_marker_inherit = PointerProperty(type = bpy.types.Object, name = "Inherit From",
	description = "Instead of supplying settings, inherits the settings of this marker instead. Good for left vs. right markers. A marker cannot inherit a marker inheriting a marker")
	bpy.types.Object.kc3dsbpy_pitch_manual = BoolProperty(name = "Manual Pitch", default = False,
	description = "Manual pitch allows specifying the angles in degrees for all 4 pitches, for front/side separately, while automatic pitch modifies a default -22.5 to 45-degree scale")
	bpy.types.Object.kc3dsbpy_pitch_mul = FloatProperty(name = "Pitch Mul", default = 1,
	description = "Multiplies the pitch given by the -22.5 to 45-degree scale")
	bpy.types.Object.kc3dsbpy_pitch_trim = FloatProperty(name = "Add", default = 0,
	description = "An angle added to the -22.5 to 45-degree scale to offset it")
	bpy.types.Object.kc3dsbpy_ppu_factor = FloatProperty(name = "Pixels Per Unit Multiplier", default = 1,
	description = "Multiplies the Pixels Per Unit while rendering this marker, important for inconsistent models and useless otherwise")
	bpy.types.Object.kc3dsbpy_visscript = StringProperty(name = "VisScript", default = "",
	description = "VisScript controls if a part can or can't be seen (see help file for language details)")
	# Pitch, manual
	bpy.types.Object.kc3dsbpy_pitch_fm1 = FloatProperty(name = "F-1", default = 22.5,
	description = "Pitch in degrees, front/back view, downward")
	bpy.types.Object.kc3dsbpy_pitch_f0 = FloatProperty(name = "F0", default = 0,
	description = "Pitch in degrees, front/back view, centre")
	bpy.types.Object.kc3dsbpy_pitch_f1 = FloatProperty(name = "F1", default = -22.5,
	description = "Pitch in degrees, front/back view, upward")
	bpy.types.Object.kc3dsbpy_pitch_f2 = FloatProperty(name = "F2", default = -45,
	description = "Pitch in degrees, front/back view, far upward")
	bpy.types.Object.kc3dsbpy_pitch_sm1 = FloatProperty(name = "S-1", default = 22.5,
	description = "Pitch in degrees, side view, downward")
	bpy.types.Object.kc3dsbpy_pitch_s0 = FloatProperty(name = "S0", default = 0,
	description = "Pitch in degrees, side view, centre")
	bpy.types.Object.kc3dsbpy_pitch_s1 = FloatProperty(name = "S1", default = -22.5,
	description = "Pitch in degrees, side view, upward")
	bpy.types.Object.kc3dsbpy_pitch_s2 = FloatProperty(name = "S2", default = -45,
	description = "Pitch in degrees, side view, far upward")
	# Data
	bpy.types.Scene.kc3dsbpy_c16_dither_colour = BoolProperty(name = "Dither C16 Colour", default = False,
	description = "!!!VERY SLOW!!! Enables Bayer 2x2 dithering. Useful if you run into banding")
	bpy.types.Scene.kc3dsbpy_c16_dither_alpha = BoolProperty(name = "Dither C16 Alpha", default = False,
	description = "!!!SLOW!!! Uses Bayer 2x2 dithering on alpha. This may screw up your sprite edges, so use with extreme care and a good compositor setup")
	bpy.types.Scene.kc3dsbpy_c16_outpath = StringProperty(name = "C16 Directory", default = "//Sprites", subtype = "DIR_PATH",
	description = "C16 files are written here")
	bpy.types.Scene.kc3dsbpy_render_bmp = BoolProperty(name = "BMP (for QuickNorn)", default = False,
	description = "If true, BMP files are written during rendering")
	bpy.types.Scene.kc3dsbpy_render_genus = EnumProperty(items = [("Norn", "Norn", "Norn"), ("Grendel", "Grendel", "Grendel"), ("Ettin", "Ettin", "Ettin"), ("Geat", "Geat", "Geat")], name = "Genus", default = "Norn",
	description = "Selects the genus to render")
	bpy.types.Scene.kc3dsbpy_render_sexes = EnumProperty(items = [("male", "Male", "Male"), ("female", "Female", "Female"), ("both", "Both", "Both")], name = "Sexes", default = "both",
	description = "Selects the sexes to render")
	bpy.types.Scene.kc3dsbpy_render_ppu = FloatProperty(name = "Pixels Per Unit", default = 100,
	description = "The Pixels Per Unit describes the scale of the scene (for adult creatures). The real value is affected by per-part PPU multipliers, so be aware of that")
	breed_slot_items = []
	for i in range(26):
		char = chr(65 + i)
		breed_slot_items.append((char.lower(), char, char))
	bpy.types.Scene.kc3dsbpy_cset = EnumProperty(items = CSETS_ITEM_LIST, name = "Template", default = "CHICHI",
	description = "Selects the template, which decides age scaling and sprite sizes")
	bpy.types.Scene.kc3dsbpy_render_breed = EnumProperty(items = breed_slot_items, name = "Slot", default = "z",
	description = "Selects the breed slot to render")
	bpy.types.Scene.kc3dsbpy_render_ages = StringProperty(name = "Ages", default = "0245",
	description = "Selects the ages to render")
	bpy.types.Scene.kc3dsbpy_render_mode = IntProperty(name = "Mode", default = 0,
	description = "Controls the 'kc3dsbpy.mode' property that exists just so you can read it")
	# uhhhhh would it be bad if I were to just use a random coin flip here
	# idk male is listed first so I guess it balances out
	bpy.types.Scene.kc3dsbpy_render_sex = EnumProperty(items = [("male", "Male", "Male"), ("female", "Female", "Female")], name = "Sex", default = "female",
	description = "Setup Frame: Controls the sex to activate for debugging")
	bpy.types.Scene.kc3dsbpy_render_age = StringProperty(name = "Age", default = "4",
	description = "Setup Frame: Selects the age to activate for debugging")
	bpy.types.Scene.kc3dsbpy_render_frame = IntProperty(name = "Frame", default = 0,
	description = "Setup Frame: Controls the frame to activate for debugging")
	# Data UI
	bpy.utils.register_class(PitchAutomaticToManualKC3DSBPY)
	bpy.utils.register_class(PitchManualToAutomaticKC3DSBPY)
	bpy.utils.register_class(CouplePartToVisKC3DSBPY)
	bpy.utils.register_class(ObjectHelpKC3DSBPY)
	bpy.utils.register_class(FrameRelativeSeekKC3DSBPY)
	bpy.utils.register_class(OBJECT_PT_ObjectPanelKC3DSBPY)
	bpy.utils.register_class(SCENE_PT_ScenePanelKC3DSBPY)

def unregister():
	# Data UI
	bpy.utils.unregister_class(PitchAutomaticToManualKC3DSBPY)
	bpy.utils.unregister_class(PitchManualToAutomaticKC3DSBPY)
	bpy.utils.unregister_class(CouplePartToVisKC3DSBPY)
	bpy.utils.unregister_class(ObjectHelpKC3DSBPY)
	bpy.utils.unregister_class(FrameRelativeSeekKC3DSBPY)
	bpy.utils.unregister_class(OBJECT_PT_ObjectPanelKC3DSBPY)
	bpy.utils.unregister_class(SCENE_PT_ScenePanelKC3DSBPY)

