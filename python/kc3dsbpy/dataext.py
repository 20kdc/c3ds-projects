#!/usr/bin/env python3
# Blender addon data, frame request calculator

# libkc3ds - General-purpose Python library for C3/DS
# Written starting in 2023 by contributors (see CREDITS.txt)
# To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
# You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

import os
import webbrowser
import shutil

import bpy
from bpy.props import StringProperty, EnumProperty, IntProperty, FloatProperty, BoolProperty, PointerProperty
from bpy.types import Operator, Panel

import libkc3ds.parts
import libkc3ds.aging
import libkc3ds.s16

from . import gizmo
from . import framereq
from . import imaging

# need to import this here, bleh
# still better than polluting the mess that is __init__
BRAND = "C3/DS Breed Exporter"

CSETS_ITEM_LIST = []

for cset in libkc3ds.aging.CSETS_ALL:
	CSETS_ITEM_LIST.append((cset.name, cset.desc, "Template: " + cset.desc))

# Part IDs
PARTIDS_ITEM_LIST = []
PARTIDS_ITEM_LIST.append(("0", "(None)", "Disabled"))
for name in libkc3ds.parts.ALL:
	# Skip unused parts. Please.
	if libkc3ds.parts.ALL[name].unused:
		continue
	PARTIDS_ITEM_LIST.append((name, name, name))

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
	bl_description = "Seeks between frames and auto-activates the frame"

	adjustment: IntProperty(name="Adjustment", description="Adjustment in frames", default=16)

	def invoke(self, context, event):
		context.scene.kc3dsbpy_render_frame += self.adjustment
		# so I'm not sure what the "NO SERIOUSLY UPDATE THIS PROPERTY" function is
		# what's certain is that for some reason, the camera's background image offset field won't be properly updated
		#  unless Gizmo reactivation happens r/n
		# something to take up with devtalk I guess, but always something else to do
		context.scene.driver_add("kc3dsbpy_render_frame")
		context.scene.driver_remove("kc3dsbpy_render_frame")
		# and actually (try to) activate the frame
		framereq.activate_frame_op(self, context)
		return {"FINISHED"}

class CopyBodyDataKC3DSBPY(Operator):
	bl_idname = "kc3dsbpy.copy_body_data"
	bl_label = "Copy Body Data"
	bl_description = "Copies ATT files from the specified source breed slot to the target breed slot, as determined by the Breed ID"

	def invoke(self, context, event):
		inpath = bpy.path.abspath(context.scene.kc3dsbpy_att_inpath)
		outpath = bpy.path.abspath(context.scene.kc3dsbpy_att_outpath)
		try:
			os.makedirs(outpath)
		except:
			pass
		cset = framereq.scene_to_cset(context.scene)
		for sex in framereq.sexes_str_to_array(context.scene.kc3dsbpy_render_sexes):
			src_sgc = libkc3ds.parts.C3_GS_MAP[sex + context.scene.kc3dsbpy_bdc_genus]
			dst_sgc = libkc3ds.parts.C3_GS_MAP[sex + context.scene.kc3dsbpy_render_genus]
			for age in context.scene.kc3dsbpy_render_ages:
				for part_info in cset.setup.part_infos:
					src_id = part_info.char + src_sgc + age + context.scene.kc3dsbpy_bdc_breed
					dst_id = part_info.char + dst_sgc + age + context.scene.kc3dsbpy_render_breed
					src_path = os.path.join(inpath, src_id + ".att")
					dst_path = os.path.join(outpath, dst_id + ".att")
					try:
						shutil.copy(src_path, dst_path)
					except:
						# there tends to be missing files (for say part '0')
						pass
		return {"FINISHED"}

class RefGenKC3DSBPY(Operator):
	bl_idname = "kc3dsbpy.generate_references"
	bl_label = "C16s -> Reference PNG sequence"
	bl_description = "Writes a single skeleton's worth of reference images"

	def invoke(self, context, event):
		inpath = bpy.path.abspath(context.scene.kc3dsbpy_ref_inpath)
		outpath = bpy.path.abspath(context.scene.kc3dsbpy_ref_outpath)
		cset = framereq.scene_to_cset(context.scene)
		frame_idx = 0
		for part_info in cset.setup.part_infos:
			c16 = []
			try:
				f = open(os.path.join(inpath, part_info.char + context.scene.kc3dsbpy_ref_xyz + ".c16"), "rb")
				data = f.read()
				f.close()
				c16 = libkc3ds.s16.decode_cs16(data)
			except:
				# oh well
				pass
			for rel_frame in range(len(part_info.frames)):
				frame_abs = rel_frame + part_info.frame_base
				res = "CA%04d.png" % frame_abs
				if rel_frame < len(c16):
					c16_frame = c16[rel_frame]
				else:
					c16_frame = libkc3ds.s16.S16Frame(1, 1)
				imaging.s16image_save_png_with_makedirs(c16_frame, os.path.join(outpath, res), alpha_aware = False)
		return {"FINISHED"}

class FrameStatus():
	def __init__(self, scene_panel, pitch_panel):
		self.scene_panel = scene_panel
		self.pitch_panel = pitch_panel

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
			pitch_panel = "(Current frame does not have pitch)"
			if "pitch_id" in frame_props and "yaw_id" in frame_props:
				pitch_panel = "Current Pitch: " + framereq.id_pitch(frame_props["pitch_id"], frame_props["yaw_id"])
		return FrameStatus(frame_status, pitch_panel)
	except:
		return FrameStatus("(unknown)", "(unknown)")

class SCENE_PT_ScenePanelKC3DSBPY(Panel):
	bl_space_type = "PROPERTIES"
	bl_region_type = "WINDOW"
	bl_context = "scene"
	bl_label = BRAND
	def draw(self, context):
		self.layout.operator(ObjectHelpKC3DSBPY.bl_idname)

class SCENE_PT_ScenePanelRiggingKC3DSBPY(Panel):
	bl_space_type = "PROPERTIES"
	bl_region_type = "WINDOW"
	bl_context = "scene"
	bl_parent_id = "SCENE_PT_ScenePanelKC3DSBPY"
	bl_label = "Rigging"
	bl_options = {"DEFAULT_CLOSED"}
	def draw(self, context):
		self.layout.prop(context.scene, "kc3dsbpy_cset")
		self.layout.prop(context.scene, "kc3dsbpy_render_ppu")

class SCENE_PT_ScenePanelRangeKC3DSBPY(Panel):
	bl_space_type = "PROPERTIES"
	bl_region_type = "WINDOW"
	bl_context = "scene"
	bl_parent_id = "SCENE_PT_ScenePanelKC3DSBPY"
	bl_label = "Breed ID"
	bl_options = {"DEFAULT_CLOSED"}
	def draw(self, context):
		row = self.layout.row()
		row.prop(context.scene, "kc3dsbpy_render_genus")
		row.prop(context.scene, "kc3dsbpy_render_breed")
		row = self.layout.row()
		row.prop(context.scene, "kc3dsbpy_render_sexes")
		row.prop(context.scene, "kc3dsbpy_render_ages")

class SCENE_PT_ScenePanelAlignmentKC3DSBPY(Panel):
	bl_space_type = "PROPERTIES"
	bl_region_type = "WINDOW"
	bl_context = "scene"
	bl_parent_id = "SCENE_PT_ScenePanelKC3DSBPY"
	bl_label = "Rig Tester"
	bl_options = {"DEFAULT_CLOSED"}
	def draw(self, context):
		row = self.layout.row()
		row.prop(context.scene, "kc3dsbpy_render_sex")
		row.prop(context.scene, "kc3dsbpy_render_age")
		row = self.layout.row()
		row.prop(context.scene, "kc3dsbpy_render_frame")
		row.label(text = calc_frame_status(context.scene).scene_panel)
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
		self.layout.prop(context.scene, "kc3dsbpy_render_disable_visscript")

class SCENE_PT_ScenePanelRenderConvertKC3DSBPY(Panel):
	bl_space_type = "PROPERTIES"
	bl_region_type = "WINDOW"
	bl_context = "scene"
	bl_parent_id = "SCENE_PT_ScenePanelKC3DSBPY"
	bl_label = "Render / Convert"
	bl_options = {"DEFAULT_CLOSED"}
	def draw(self, context):
		self.layout.prop(context.scene, "kc3dsbpy_render_bmp")
		row = self.layout.row()
		row.operator("kc3dsbpy.render")
		row.prop(context.scene, "kc3dsbpy_render_mode")
		self.layout.separator()
		row = self.layout.row()
		row.prop(context.scene, "kc3dsbpy_c16_dither_colour")
		row.prop(context.scene, "kc3dsbpy_c16_dither_alpha")
		self.layout.prop(context.scene, "kc3dsbpy_c16_outpath")
		row = self.layout.row()
		row.operator("kc3dsbpy.png2c16")
		row.operator("kc3dsbpy.make_sheets")

class SCENE_PT_ScenePanelBodyDataCopyKC3DSBPY(Panel):
	bl_space_type = "PROPERTIES"
	bl_region_type = "WINDOW"
	bl_context = "scene"
	bl_parent_id = "SCENE_PT_ScenePanelKC3DSBPY"
	bl_label = "Body Data"
	bl_options = {"DEFAULT_CLOSED"}
	def draw(self, context):
		self.layout.prop(context.scene, "kc3dsbpy_att_inpath", text = "Src.")
		row = self.layout.row()
		row.prop(context.scene, "kc3dsbpy_bdc_genus", text = "Genus")
		row.prop(context.scene, "kc3dsbpy_bdc_breed", text = "Slot")
		self.layout.prop(context.scene, "kc3dsbpy_att_outpath", text = "Dest.")
		self.layout.operator(CopyBodyDataKC3DSBPY.bl_idname)
		self.layout.operator("kc3dsbpy.compile_body_data")

class SCENE_PT_ScenePanelRefGenKC3DSBPY(Panel):
	bl_space_type = "PROPERTIES"
	bl_region_type = "WINDOW"
	bl_context = "scene"
	bl_parent_id = "SCENE_PT_ScenePanelKC3DSBPY"
	bl_label = "Reference Image Generator"
	bl_options = {"DEFAULT_CLOSED"}
	def draw(self, context):
		self.layout.prop(context.scene, "kc3dsbpy_ref_inpath", text = "C16s In")
		self.layout.prop(context.scene, "kc3dsbpy_ref_outpath", text = "PNGs Out")
		self.layout.prop(context.scene, "kc3dsbpy_ref_xyz")
		self.layout.label(text = "Suffixes don't include part ID: 'a04d.c16' has XYZ '04d'")
		self.layout.label(text = "For more information see 'Breed' on Creatures Wiki")
		self.layout.operator(RefGenKC3DSBPY.bl_idname)

class OBJECT_PT_ObjectPanelKC3DSBPY(Panel):
	bl_space_type = "PROPERTIES"
	bl_region_type = "WINDOW"
	bl_context = "object"
	bl_label = BRAND
	def draw(self, context):
		row = self.layout.row()
		row.prop(context.object, "kc3dsbpy_part_marker")
		row.operator(CouplePartToVisKC3DSBPY.bl_idname)
		part_name = context.object.kc3dsbpy_part_marker
		if part_name != "0":
			self.layout.prop(context.object, "kc3dsbpy_part_role")
			role = context.object.kc3dsbpy_part_role
			if role == "MARKER":
				self.layout.prop(context.object, "kc3dsbpy_marker_inherit")
				if context.object.kc3dsbpy_marker_inherit is None:
					if context.object.kc3dsbpy_pitch_manual:
						row = self.layout.row()
						row.prop(context.object, "kc3dsbpy_pitch_manual")
						row.operator(PitchManualToAutomaticKC3DSBPY.bl_idname)
						row = self.layout.row()
						row.prop(context.object, "kc3dsbpy_pitch_sm1")
						row.prop(context.object, "kc3dsbpy_pitch_fm1")
						row = self.layout.row()
						row.prop(context.object, "kc3dsbpy_pitch_s0")
						row.prop(context.object, "kc3dsbpy_pitch_f0")
						row = self.layout.row()
						row.prop(context.object, "kc3dsbpy_pitch_s1")
						row.prop(context.object, "kc3dsbpy_pitch_f1")
						row = self.layout.row()
						row.prop(context.object, "kc3dsbpy_pitch_s2")
						row.prop(context.object, "kc3dsbpy_pitch_f2")
					else:
						row = self.layout.row()
						row.prop(context.object, "kc3dsbpy_pitch_manual")
						row.operator(PitchAutomaticToManualKC3DSBPY.bl_idname)
						row = self.layout.row()
						row.prop(context.object, "kc3dsbpy_pitch_mul")
						row.prop(context.object, "kc3dsbpy_pitch_trim")
					self.layout.label(text = calc_frame_status(context.scene).pitch_panel)
					self.layout.prop(context.object, "kc3dsbpy_ppu_factor")
			elif role in gizmo.ATT_ROLES:
				att_role_idx = gizmo.ATT_ROLES[role]
				# Show ATT role info
				cset = framereq.scene_to_cset(context.scene)
				point_name = "<unknown>"
				if part_name in cset.setup.part_names_to_infos:
					part_info = cset.setup.part_names_to_infos[part_name]
					if att_role_idx < len(part_info.att_point_names):
						point_name = part_info.att_point_names[att_role_idx]
				location = ""
				try:
					location = gizmo.get_att_outside_of_gizmo(context, context.scene, context.scene.camera, context.object)
					location = str(location)
				except:
					pass
				self.layout.label(text = point_name + " " + location)
		self.layout.prop(context.object, "kc3dsbpy_visscript")
		self.layout.operator(ObjectHelpKC3DSBPY.bl_idname)

# Registration and stuff

def register():
	# Kind of shared with Gizmo but will just have to live with it due to the items
	bpy.types.Object.kc3dsbpy_part_marker = EnumProperty(items = PARTIDS_ITEM_LIST, name = "Part", default = "0")
	bpy.types.Object.kc3dsbpy_part_role = EnumProperty(items = [
		("MARKER", "Marker", "The camera uses this object as the reference point for rendering images of the given part. In addition, this object will be rotated for pitch/yaw, and this object acts as the per-part settings. There can only be one of these for each part in the scene."),
		("ATT0", "ATT[0]", "Attachment point 0"),
		("ATT1", "ATT[1]", "Attachment point 1"),
		("ATT2", "ATT[2]", "Attachment point 2"),
		("ATT3", "ATT[3]", "Attachment point 3"),
		("ATT4", "ATT[4]", "Attachment point 4"),
		("ATT5", "ATT[5]", "Attachment point 5")
	], name = "Role", default = "MARKER",
	description = "Role of this object for the given part")
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
	bpy.types.Scene.kc3dsbpy_render_disable_visscript = BoolProperty(name = "Disable VisScript", default = False,
	description = "Disables VisScript. You probably do not want to just leave this enabled, especially unless making a Geat")
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
	# BDC
	bpy.types.Scene.kc3dsbpy_bdc_genus = EnumProperty(items = [("Norn", "Norn", "Norn"), ("Grendel", "Grendel", "Grendel"), ("Ettin", "Ettin", "Ettin"), ("Geat", "Geat", "Geat")], name = "Source Genus", default = "Norn",
	description = "Selects the genus to copy body data from")
	bpy.types.Scene.kc3dsbpy_bdc_breed = EnumProperty(items = breed_slot_items, name = "Source Breed Slot", default = "d",
	description = "Selects the breed slot to copy body data from")
	bpy.types.Scene.kc3dsbpy_att_inpath = StringProperty(name = "Body Data Input Dir.", default = "//Body Data", subtype = "DIR_PATH",
	description = "ATT files are read from here")
	bpy.types.Scene.kc3dsbpy_att_outpath = StringProperty(name = "Body Data Output Dir.", default = "//Body Data", subtype = "DIR_PATH",
	description = "ATT files are written here")
	bpy.types.Scene.kc3dsbpy_ref_inpath = StringProperty(name = "Ref. Input Dir.", default = "//Images", subtype = "DIR_PATH",
	description = "Reference C16 files are read from here")
	bpy.types.Scene.kc3dsbpy_ref_outpath = StringProperty(name = "Ref. Output Dir.", default = "//ref", subtype = "DIR_PATH",
	description = "Reference PNGs are written here")
	bpy.types.Scene.kc3dsbpy_ref_xyz = StringProperty(name = "'XYZ' Suffix", default = "04d",
	description = "i.e. '04d' for Norn Male D")
	# Data UI
	bpy.utils.register_class(PitchAutomaticToManualKC3DSBPY)
	bpy.utils.register_class(PitchManualToAutomaticKC3DSBPY)
	bpy.utils.register_class(CouplePartToVisKC3DSBPY)
	bpy.utils.register_class(ObjectHelpKC3DSBPY)
	bpy.utils.register_class(FrameRelativeSeekKC3DSBPY)
	bpy.utils.register_class(CopyBodyDataKC3DSBPY)
	bpy.utils.register_class(RefGenKC3DSBPY)
	# -
	bpy.utils.register_class(OBJECT_PT_ObjectPanelKC3DSBPY)
	bpy.utils.register_class(SCENE_PT_ScenePanelKC3DSBPY)
	# order matters here {
	bpy.utils.register_class(SCENE_PT_ScenePanelRangeKC3DSBPY)
	bpy.utils.register_class(SCENE_PT_ScenePanelRiggingKC3DSBPY)
	bpy.utils.register_class(SCENE_PT_ScenePanelAlignmentKC3DSBPY)
	bpy.utils.register_class(SCENE_PT_ScenePanelRenderConvertKC3DSBPY)
	bpy.utils.register_class(SCENE_PT_ScenePanelBodyDataCopyKC3DSBPY)
	bpy.utils.register_class(SCENE_PT_ScenePanelRefGenKC3DSBPY)
	# }

def unregister():
	# Data UI
	bpy.utils.unregister_class(PitchAutomaticToManualKC3DSBPY)
	bpy.utils.unregister_class(PitchManualToAutomaticKC3DSBPY)
	bpy.utils.unregister_class(CouplePartToVisKC3DSBPY)
	bpy.utils.unregister_class(ObjectHelpKC3DSBPY)
	bpy.utils.unregister_class(FrameRelativeSeekKC3DSBPY)
	bpy.utils.unregister_class(CopyBodyDataKC3DSBPY)
	bpy.utils.unregister_class(RefGenKC3DSBPY)
	# -
	bpy.utils.unregister_class(OBJECT_PT_ObjectPanelKC3DSBPY)
	bpy.utils.unregister_class(SCENE_PT_ScenePanelKC3DSBPY)
	bpy.utils.unregister_class(SCENE_PT_ScenePanelRiggingKC3DSBPY)
	bpy.utils.unregister_class(SCENE_PT_ScenePanelRangeKC3DSBPY)
	bpy.utils.unregister_class(SCENE_PT_ScenePanelAlignmentKC3DSBPY)
	bpy.utils.unregister_class(SCENE_PT_ScenePanelRenderConvertKC3DSBPY)
	bpy.utils.unregister_class(SCENE_PT_ScenePanelBodyDataCopyKC3DSBPY)
	bpy.utils.unregister_class(SCENE_PT_ScenePanelRefGenKC3DSBPY)

