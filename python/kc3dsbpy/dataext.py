#!/usr/bin/env python3
# Blender addon data

# libkc3ds - General-purpose Python library for C3/DS
# Written starting in 2023 by contributors (see CREDITS.txt)
# To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
# You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

import bpy

import libkc3ds.parts

from bpy.props import StringProperty, EnumProperty
from bpy.types import Operator, Panel

import webbrowser

# need to import this here, bleh
# still better than polluting the mess that is __init__
BRAND = "C3/DS Breed Exporter"

# Data UI

class ObjectHelpKC3DSBPY(Operator):
	bl_idname = "kc3dsbpy.objecthelp"
	bl_label = "Help"
	bl_description = "View help for kc3dsbpy."

	def invoke(self, context, event):
		webbrowser.open("https://github.com/20kdc/c3ds-projects/blob/main/python/kc3dsbpy/HELP.md")
		return {"FINISHED"}

class CouplePartToVisKC3DSBPY(Operator):
	bl_idname = "kc3dsbpy.coupleparttovis"
	bl_label = "Into VisScript"
	bl_description = "Sets the VisScript so this Object is only visible if the current part is that set as the Marker."

	def invoke(self, context, event):
		context.object.kc3dsbpy_visscript = "part=" + context.object.kc3dsbpy_part_marker
		return {"FINISHED"}

class SCENE_PT_ScenePanelKC3DSBPY(Panel):
	bl_space_type = "PROPERTIES"
	bl_region_type = "WINDOW"
	bl_context = "scene"
	bl_label = BRAND
	def draw(self, context):
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
	# Data
	bpy.types.Object.kc3dsbpy_part_marker = EnumProperty(items = all_part_ids, name = "Marker", default = "")
	bpy.types.Object.kc3dsbpy_visscript = StringProperty(name = "VisScript", default = "")
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

