#!/usr/bin/env python3
# Blender addon data, frame request calculator

# libkc3ds - General-purpose Python library for C3/DS
# Written starting in 2023 by contributors (see CREDITS.txt)
# To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
# You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

import os
import webbrowser

import bpy
from bpy.props import StringProperty, EnumProperty, IntProperty, FloatProperty, BoolProperty
from bpy.types import Operator, Panel

import libkc3ds.parts

from . import gizmo
from . import chichi

# need to import this here, bleh
# still better than polluting the mess that is __init__
BRAND = "C3/DS Breed Exporter"

# Skeleton Request Calculator

class SkeletonReqContext():
	"""
	Contains information collated for a part.
	"""
	def __init__(self, gizmo_context, cset, sex, age_char):
		scene = gizmo_context.scene
		self.setup = cset.setup
		self.gizmo_context = gizmo_context
		self.path_gb = os.path.join(scene.kc3dsbpy_render_genus, scene.kc3dsbpy_render_breed, sex, age_char)
		self.age_data = cset.ages[age_char]
		self.pixels_per_unit = scene.kc3dsbpy_render_ppu
		self.props = {
			"genus": scene.kc3dsbpy_render_genus,
			"breed": scene.kc3dsbpy_render_breed,
			"male": 0,
			"female": 0,
			"age": self.age_data.val
		}
		self.props[sex] = 1
		# Last 3 characters as per QuickNorn.
		self.xyz = libkc3ds.parts.C3_GS_MAP[sex + scene.kc3dsbpy_render_genus] + age_char + scene.kc3dsbpy_render_breed

	def frame_req(self, frame_props):
		"""
		Takes frame properties from libkc3ds and turns them into a resolved FrameReq.
		"""
		# attach frame props to skeleton props
		new_props = self.props.copy()
		for k in frame_props:
			new_props[k] = frame_props[k]
		# get this
		part_name = new_props["part"]
		# calculate file paths
		cv = os.path.join(self.path_gb, "CV%04d" % new_props["frame"])
		path_png = cv + ".png"
		path_bmp = cv + ".bmp"
		part_char = self.setup.part_names_to_infos[part_name].char
		path_c16 = part_char + self.xyz + ".c16"
		paths = ReqPaths(path_png, path_bmp, path_c16, new_props["frame_rel"])
		# check part name exists as a marker, if not, we'll have to skip
		if not (part_name in self.gizmo_context.markers):
			return BlankReq(part_name, paths)
		# infuse age data
		aged_part = self.age_data.parts[new_props["part"]]
		new_props["width"] = aged_part.size
		new_props["height"] = aged_part.size
		new_props["ortho_scale"] = aged_part.size / (self.age_data.scale * self.pixels_per_unit)
		# infuse rotation data
		new_props["pitch"] = new_props["pitch_id"] * -22.5
		new_props["yaw"] = new_props["yaw_id"] * 90
		new_props["roll"] = 0
		return FrameReq(self.gizmo_context, new_props, part_name, paths)

class ReqPaths():
	"""
	Describes file paths.
	"""
	def __init__(self, path_png, path_bmp, path_c16, frame_c16):
		self.png = path_png
		self.bmp = path_bmp
		self.c16 = path_c16
		self.c16_frame = frame_c16

	def __str__(self):
		return self.png + " (" + self.c16 + "/" + str(self.c16_frame) + ")"

class BlankReq():
	"""
	Describes a blank frame request.
	"""
	def __init__(self, part_name, paths):
		self.part_name = part_name
		self.paths = paths

class FrameReq(BlankReq):
	"""
	Describes a single frame request.
	Frame requests must be in C16 order so they collate properly.
	"""
	def __init__(self, gizmo_context, gizmo_props, part_name, paths):
		super().__init__(part_name, paths)
		self.gizmo_context = gizmo_context
		self.gizmo_props = gizmo_props
		self.gizmo_context.verify(gizmo_props)

	def activate(self):
		self.gizmo_context.activate(self.gizmo_props)

	def deactivate(self):
		self.gizmo_context.deactivate()

def sexes_str_to_array(sexes):
	if sexes == "both":
		return ["male", "female"]
	return [sexes]

def calc_req_group(scene):
	"""
	Calculates an all-frame operation requested by the given Scene.
	"""
	gizmo_context = gizmo.GizmoContext(scene)
	cset = chichi.CHICHI
	group = []
	for sex in sexes_str_to_array(scene.kc3dsbpy_render_sexes):
		for age_char in scene.kc3dsbpy_render_ages:
			frc = SkeletonReqContext(gizmo_context, cset, sex, age_char)
			for frame_props in cset.setup.frames:
				group.append(frc.frame_req(frame_props))
	return group

def calc_req_frame(scene):
	"""
	Calculates a single-frame operation requested by the given Scene.
	"""
	gizmo_context = gizmo.GizmoContext(scene)
	cset = chichi.CHICHI
	frc = SkeletonReqContext(gizmo_context, cset, scene.kc3dsbpy_render_sex, scene.kc3dsbpy_render_age)
	return frc.frame_req(cset.setup.frames[scene.kc3dsbpy_render_frame])

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

class SCENE_PT_ScenePanelKC3DSBPY(Panel):
	bl_space_type = "PROPERTIES"
	bl_region_type = "WINDOW"
	bl_context = "scene"
	bl_label = BRAND
	def draw(self, context):
		self.layout.prop(context.scene, "kc3dsbpy_render_c16")
		self.layout.prop(context.scene, "kc3dsbpy_render_bmp")
		self.layout.prop(context.scene, "kc3dsbpy_render_genus")
		self.layout.prop(context.scene, "kc3dsbpy_render_breed")
		self.layout.prop(context.scene, "kc3dsbpy_render_sexes")
		self.layout.prop(context.scene, "kc3dsbpy_render_ages")
		self.layout.prop(context.scene, "kc3dsbpy_render_ppu")
		self.layout.operator("kc3dsbpy.render")
		self.layout.prop(context.scene, "kc3dsbpy_render_sex")
		self.layout.prop(context.scene, "kc3dsbpy_render_age")
		self.layout.prop(context.scene, "kc3dsbpy_render_frame")
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
		self.layout.prop(context.object, "kc3dsbpy_visscript")
		self.layout.operator(ObjectHelpKC3DSBPY.bl_idname)

# Registration and stuff

def register():
	# Part IDs
	all_part_ids = []
	all_part_ids.append(("", "(None)", "Disabled"))
	for name in libkc3ds.parts.ALL:
		all_part_ids.append((name, name, "Used as camera location for part: " + name))
	# Kind of shared with Gizmo but will just have to live with it due to the items
	bpy.types.Object.kc3dsbpy_part_marker = EnumProperty(items = all_part_ids, name = "Marker", default = "")
	bpy.types.Object.kc3dsbpy_visscript = StringProperty(name = "VisScript", default = "")
	# Data
	bpy.types.Scene.kc3dsbpy_render_c16 = BoolProperty(name = "C16 (TODO!)", default = False)
	bpy.types.Scene.kc3dsbpy_render_bmp = BoolProperty(name = "BMP (for QuickNorn) (TODO!)", default = False)
	bpy.types.Scene.kc3dsbpy_render_genus = EnumProperty(items = [("Norn", "Norn", "Norn"), ("Grendel", "Grendel", "Grendel"), ("Ettin", "Ettin", "Ettin"), ("Geat", "Geat", "Geat")], name = "Genus", default = "Norn")
	bpy.types.Scene.kc3dsbpy_render_sexes = EnumProperty(items = [("male", "Male", "Male"), ("female", "Female", "Female"), ("both", "Both", "Both")], name = "Sexes", default = "both")
	bpy.types.Scene.kc3dsbpy_render_ppu = FloatProperty(name = "Pixels Per Unit", description = "Pixels Per Unit (for adult creatures)", default = 100)
	breed_slot_items = []
	for i in range(26):
		char = chr(65 + i)
		breed_slot_items.append((char.lower(), char, char))
	bpy.types.Scene.kc3dsbpy_render_breed = EnumProperty(items = breed_slot_items, name = "Breed Slot", default = "z")
	bpy.types.Scene.kc3dsbpy_render_ages = StringProperty(name = "Ages", default = "0245")
	bpy.types.Scene.kc3dsbpy_render_age = StringProperty(name = "Age", default = "4")
	# uhhhhh would it be bad if I were to just use a random coin flip here
	# idk male is listed first so I guess it balances out
	bpy.types.Scene.kc3dsbpy_render_sex = EnumProperty(items = [("male", "Male", "Male"), ("female", "Female", "Female")], name = "Sex", default = "female")
	bpy.types.Scene.kc3dsbpy_render_frame = IntProperty(name = "Frame", default = 0)
	# Data UI
	bpy.utils.register_class(CouplePartToVisKC3DSBPY)
	bpy.utils.register_class(ObjectHelpKC3DSBPY)
	bpy.utils.register_class(OBJECT_PT_ObjectPanelKC3DSBPY)
	bpy.utils.register_class(SCENE_PT_ScenePanelKC3DSBPY)

def unregister():
	# Data UI
	bpy.utils.unregister_class(CouplePartToVisKC3DSBPY)
	bpy.utils.unregister_class(ObjectHelpKC3DSBPY)
	bpy.utils.unregister_class(OBJECT_PT_ObjectPanelKC3DSBPY)
	bpy.utils.unregister_class(SCENE_PT_ScenePanelKC3DSBPY)

