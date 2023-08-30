#!/usr/bin/env python3
# Blender addon data

# caosprox - CPX server reference implementation
# Written starting in 2022 by contributors (see CREDITS.txt)
# To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
# You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

import bpy

from bpy.props import StringProperty, EnumProperty

# Data UI

class CoupleLimbToVisKC3DSBPY(Operator):
	bl_idname = "kc3dsbpy.couplelimbtovis"
	bl_label = "Limb -> VisScript"
	bl_description = "Couples Limb to VisScript"

	def invoke(self, context, event):
		context.object.kc3dsbpy_visscript = "limb=" + context.object.kc3dsbpy_limb_marker
		path_base = os.path.join(bpy.path.abspath(context.scene.render.filepath), "Norn", "z")
		frames = []
		# TODO: Actually allow configuring any of this
		chichi.CHICHI.ages[0].gen_frames(frames, {"filepath": path_base, "male": 1, "female": 0})
		render_gizmo_frames(context, path_base, frames)
		return {"FINISHED"}

class ObjectPanelKC3DSBPY(Panel):
	bl_space_type = "PROPERTIES"
	bl_region_type = "WINDOW"
	bl_context = "object"
	bl_label = BRAND
	def draw(self, context):
		self.layout.prop(context.object, "kc3dsbpy_limb_marker")
		self.layout.prop(context.object, "kc3dsbpy_visscript")
		self.layout.operator(CoupleLimbToVisKC3DSBPY.bl_idname)
		self.layout.label(text = "See kc3dsbpy documentation for details")

def register():
	# Data
	bpy.types.Object.kc3dsbpy_limb_marker = EnumProperty(["None", ], name = "Limb Marker", default = "")
	bpy.types.Object.kc3dsbpy_visscript = StringProperty(name = "VisScript", default = "")
	# Data UI
	bpy.utils.register_class(ObjectPanelKC3DSBPY)

def unregister():
	# Data UI
	bpy.utils.unregister_class(ObjectPanelKC3DSBPY)

