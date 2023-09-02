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
	bl_description = "Converts automatic pitch to manual pitch."

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
	bl_description = "Converts manual pitch to automatic pitch, based on FM1 and F2."

	def invoke(self, context, event):
		context.object.kc3dsbpy_pitch_manual = False
		context.object.kc3dsbpy_pitch_mul = framereq.calc_pitch_mul(context.object.kc3dsbpy_pitch_fm1, context.object.kc3dsbpy_pitch_f2)
		context.object.kc3dsbpy_pitch_trim = framereq.calc_pitch_trim(context.object.kc3dsbpy_pitch_fm1, context.object.kc3dsbpy_pitch_f2)
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
		row.operator("kc3dsbpy.activate_frame")
		row.operator("kc3dsbpy.deactivate_frame")
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
	bpy.types.Object.kc3dsbpy_part_marker = EnumProperty(items = all_part_ids, name = "Marker", default = "0")
	bpy.types.Object.kc3dsbpy_marker_inherit = PointerProperty(type = bpy.types.Object, name = "Inherit From", description = "Will use properties of this marker instead")
	bpy.types.Object.kc3dsbpy_pitch_manual = BoolProperty(name = "Manual Pitch", default = False)
	bpy.types.Object.kc3dsbpy_pitch_mul = FloatProperty(name = "Pitch Mul", default = 1)
	bpy.types.Object.kc3dsbpy_pitch_trim = FloatProperty(name = "Add", default = 0)
	bpy.types.Object.kc3dsbpy_ppu_factor = FloatProperty(name = "Pixels Per Unit Multiplier", description = "Multiplies the Pixels Per Unit while rendering this marker", default = 1)
	bpy.types.Object.kc3dsbpy_visscript = StringProperty(name = "VisScript", default = "")
	# Pitch, manual
	bpy.types.Object.kc3dsbpy_pitch_fm1 = FloatProperty(name = "F-1", default = -22.5)
	bpy.types.Object.kc3dsbpy_pitch_f0 = FloatProperty(name = "F0", default = 0)
	bpy.types.Object.kc3dsbpy_pitch_f1 = FloatProperty(name = "F1", default = 22.5)
	bpy.types.Object.kc3dsbpy_pitch_f2 = FloatProperty(name = "F2", default = 45)
	bpy.types.Object.kc3dsbpy_pitch_sm1 = FloatProperty(name = "S-1", default = -22.5)
	bpy.types.Object.kc3dsbpy_pitch_s0 = FloatProperty(name = "S0", default = 0)
	bpy.types.Object.kc3dsbpy_pitch_s1 = FloatProperty(name = "S1", default = 22.5)
	bpy.types.Object.kc3dsbpy_pitch_s2 = FloatProperty(name = "S2", default = 45)
	# Data
	bpy.types.Scene.kc3dsbpy_c16_dither_colour = BoolProperty(name = "Dither C16 Colour", default = False)
	bpy.types.Scene.kc3dsbpy_c16_dither_alpha = BoolProperty(name = "Dither C16 Alpha", default = False)
	bpy.types.Scene.kc3dsbpy_c16_outpath = StringProperty(name = "C16 Output Directory", default = "//Sprites", subtype = "DIR_PATH")
	bpy.types.Scene.kc3dsbpy_render_bmp = BoolProperty(name = "BMP (for QuickNorn)", default = False)
	bpy.types.Scene.kc3dsbpy_render_genus = EnumProperty(items = [("Norn", "Norn", "Norn"), ("Grendel", "Grendel", "Grendel"), ("Ettin", "Ettin", "Ettin"), ("Geat", "Geat", "Geat")], name = "Genus", default = "Norn")
	bpy.types.Scene.kc3dsbpy_render_sexes = EnumProperty(items = [("male", "Male", "Male"), ("female", "Female", "Female"), ("both", "Both", "Both")], name = "Sexes", default = "both")
	bpy.types.Scene.kc3dsbpy_render_ppu = FloatProperty(name = "Pixels Per Unit", description = "Pixels Per Unit (for adult creatures)", default = 100)
	breed_slot_items = []
	for i in range(26):
		char = chr(65 + i)
		breed_slot_items.append((char.lower(), char, char))
	bpy.types.Scene.kc3dsbpy_render_breed = EnumProperty(items = breed_slot_items, name = "Slot", default = "z")
	bpy.types.Scene.kc3dsbpy_render_ages = StringProperty(name = "Ages", default = "0245")
	bpy.types.Scene.kc3dsbpy_render_age = StringProperty(name = "Age", default = "4")
	bpy.types.Scene.kc3dsbpy_render_mode = IntProperty(name = "Mode", default = 0)
	# uhhhhh would it be bad if I were to just use a random coin flip here
	# idk male is listed first so I guess it balances out
	bpy.types.Scene.kc3dsbpy_render_sex = EnumProperty(items = [("male", "Male", "Male"), ("female", "Female", "Female")], name = "Sex", default = "female")
	bpy.types.Scene.kc3dsbpy_render_frame = IntProperty(name = "Frame", default = 0)
	bpy.types.Scene.kc3dsbpy_cset = EnumProperty(items = CSETS_ITEM_LIST, name = "Template", default = "CHICHI")
	# Data UI
	bpy.utils.register_class(PitchAutomaticToManualKC3DSBPY)
	bpy.utils.register_class(PitchManualToAutomaticKC3DSBPY)
	bpy.utils.register_class(CouplePartToVisKC3DSBPY)
	bpy.utils.register_class(ObjectHelpKC3DSBPY)
	bpy.utils.register_class(OBJECT_PT_ObjectPanelKC3DSBPY)
	bpy.utils.register_class(SCENE_PT_ScenePanelKC3DSBPY)

def unregister():
	# Data UI
	bpy.utils.unregister_class(PitchAutomaticToManualKC3DSBPY)
	bpy.utils.unregister_class(PitchManualToAutomaticKC3DSBPY)
	bpy.utils.unregister_class(CouplePartToVisKC3DSBPY)
	bpy.utils.unregister_class(ObjectHelpKC3DSBPY)
	bpy.utils.unregister_class(OBJECT_PT_ObjectPanelKC3DSBPY)
	bpy.utils.unregister_class(SCENE_PT_ScenePanelKC3DSBPY)

