#!/usr/bin/env python3
# Decomplicates things, I promise.

# libkc3ds - General-purpose Python library for C3/DS
# Written starting in 2023 by contributors (see CREDITS.txt)
# To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
# You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

import bpy
from bpy_extras.object_utils import world_to_camera_view
import mathutils
import math

from . import visscript

ATT_ROLES = {
	"ATT0": 0,
	"ATT1": 1,
	"ATT2": 2,
	"ATT3": 3,
	"ATT4": 4,
	"ATT5": 5
}

# See visscript section of HELP.md

class VisScriptBPYCtx():
	def __init__(self, ctx):
		self.ctx = ctx
	def exists(self, prop):
		return ("kc3dsbpy." + prop) in self.ctx
	def get(self, prop):
		return str(self.ctx["kc3dsbpy." + prop])

def visscript_compile_and_bind(scene, obj):
	"""
	Creates a lambda which updates the object rendering status from Gizmo properties.
	"""
	tkns = visscript.visscript_tokenize(obj.kc3dsbpy_visscript)
	compiled = visscript.visscript_compile(tkns)
	scene_vsc = VisScriptBPYCtx(scene)
	def bound():
		obj.hide_render = not compiled(scene_vsc)
		obj.hide_viewport = obj.hide_render
	return bound

def get_att_outside_of_gizmo(context, scene, camera, obj):
	"""
	Returns the ATT (X, Y) tuple for a given scene/camera/point as objects.
	"""
	# firstly, map these to evaluated objects
	graph = context.evaluated_depsgraph_get()
	scene_ev = scene.evaluated_get(graph)
	camera_ev = camera.evaluated_get(graph)
	obj_ev = obj.evaluated_get(graph)
	# now then, where were we
	vec = world_to_camera_view(scene_ev, camera_ev, obj_ev.matrix_world.translation)
	# flip & scale (this uses the resolution "as Gizmo sees it" for now)
	x = (vec[0]) * scene.render.resolution_x
	y = (1 - vec[1]) * scene.render.resolution_y
	# fit
	x = round(x)
	y = round(y)
	return (x, y)

GIZMO_CONSTRAINT_PREFIX = "AUTO_DELETE:KC3DSBPY:"

class GizmoContext():
	"""
	A scan of the scene, etc.
	"""
	def __init__(self, scene):
		self.scene = scene
		self.camera = scene.camera
		self.markers = {}
		# [marker][point]
		self.att_points = {}
		if self.camera is None:
			raise Exception("Camera required!")
		self.vis = []
		for obj in scene.objects:
			mk = obj.kc3dsbpy_part_marker
			if mk != "" and mk != "0":
				role = obj.kc3dsbpy_part_role
				if role == "MARKER":
					if mk in self.markers:
						raise Exception("Duplicate marker: " + self.markers[mk].name + " to " + obj.name)
					self.markers[mk] = obj
				elif role in ATT_ROLES:
					att_role_idx = ATT_ROLES[role]
					if not (mk in self.att_points):
						self.att_points[mk] = {}
					outer = self.att_points[mk]
					if att_role_idx in outer:
						conflict = outer[att_role_idx].name
						raise Exception("Duplicate ATT point: " + conflict + " to " + obj.name)
					outer[att_role_idx] = obj
			self.vis.append(visscript_compile_and_bind(scene, obj))

	def verify(self, props):
		"""
		Checks that the given operation is possible.
		"""
		marker = props["part"]
		if not marker in self.markers:
			raise Exception("Marker " + marker + " does not exist")
		burn = props["pitch"]
		burn = props["roll"]
		burn = props["yaw"]
		burn = props["width"]
		burn = props["height"]
		burn = props["ortho_scale"]

	def activate(self, props):
		self.deactivate()
		self.backup()
		# Mirror all Gizmo properties to scene custom properties.
		# This can be used to run Drivers for instance.
		for k in props:
			self.scene["kc3dsbpy." + k] = props[k]
		# Setup target.
		marker = self.markers[props["part"]]
		marker.rotation_euler = mathutils.Euler((math.radians(props["pitch"]), math.radians(props["roll"]), math.radians(props["yaw"])), "YXZ")
		# Setup camera/resolution.
		self.scene.render.resolution_x = props["width"]
		self.scene.render.resolution_y = props["height"]
		self.camera.data.ortho_scale = props["ortho_scale"]
		# Camera teleport was previously handled by literally editing location
		# However this made alignment awkward and non-responsive
		# Only way to keep rigging compatibility while cleaning this up is to:
		# * Use a copy location constraint with offset
		#    BUT that also applies camera.location!!!
		# * So: Reset cam location to zero
		#    Previous versions will have left non-zero cam locations in file
		#    Offset would affect these and the Space options don't let us fix it
		#    (applies rotation = very bad no-good)
		#    So essentially this patches the rig for the new addon version
		self.camera.location = mathutils.Vector((0, 0, 0))
		camera_location_constraint = self.camera.constraints.new("COPY_LOCATION")
		camera_location_constraint.name = GIZMO_CONSTRAINT_PREFIX + "CAMERA_RIG"
		camera_location_constraint.target = marker
		camera_location_constraint.use_offset = True
		# Setup visibility.
		for vis in self.vis:
			vis()

	def backup(self):
		"""
		Part of activation, performs backups.
		"""
		self.scene.kc3dsbpy_gizmo_resx_old = self.scene.render.resolution_x
		self.scene.kc3dsbpy_gizmo_resy_old = self.scene.render.resolution_y
		self.scene.kc3dsbpy_gizmo_activated = True
		for obj in self.scene.objects:
			obj.kc3dsbpy_gizmo_activated = True
			obj.kc3dsbpy_gizmo_hide_render_old = obj.hide_render
			obj.kc3dsbpy_gizmo_hide_viewport_old = obj.hide_viewport
			obj.kc3dsbpy_gizmo_ex_old = obj.rotation_euler.x
			obj.kc3dsbpy_gizmo_ey_old = obj.rotation_euler.y
			obj.kc3dsbpy_gizmo_ez_old = obj.rotation_euler.z
			obj.kc3dsbpy_gizmo_et_old = obj.rotation_euler.order

	def get_att(self, context, marker, point_idx):
		"""
		Returns the ATT X,Y tuple for a given marker/point, or (0, 0)
		Only do this while activated (or at least try to).
		Like context creation, read-only so it's safe to use anytime.
		"""
		if not (marker in self.att_points):
			return (0, 0)
		outer = self.att_points[marker]
		if not (point_idx in outer):
			return (0, 0)
		obj = outer[point_idx]
		return get_att_outside_of_gizmo(context, self.scene, self.camera, obj)

	def deactivate(self):
		"""
		Clean up the results of any Gizmo activation.
		"""
		if self.scene.kc3dsbpy_gizmo_activated:
			self.scene.render.resolution_x = self.scene.kc3dsbpy_gizmo_resx_old
			self.scene.render.resolution_y = self.scene.kc3dsbpy_gizmo_resy_old
			self.scene.kc3dsbpy_gizmo_activated = False
		for obj in self.scene.objects:
			if obj.kc3dsbpy_gizmo_activated:
				# remove any constraints created by Gizmo, and ONLY those constraints,
				# and ONLY on Gizmo-activated objects
				remove_these_constraints = []
				for constraint in obj.constraints:
					if constraint.name.startswith(GIZMO_CONSTRAINT_PREFIX):
						remove_these_constraints.append(constraint)
				for constraint in remove_these_constraints:
					obj.constraints.remove(constraint)
				# continue!
				obj.hide_render = obj.kc3dsbpy_gizmo_hide_render_old
				obj.hide_viewport = obj.kc3dsbpy_gizmo_hide_viewport_old
				obj.rotation_euler = mathutils.Euler((obj.kc3dsbpy_gizmo_ex_old, obj.kc3dsbpy_gizmo_ey_old, obj.kc3dsbpy_gizmo_ez_old), obj.kc3dsbpy_gizmo_et_old)
				obj.kc3dsbpy_gizmo_activated = False

class DeactivateFKC3DSBPY(bpy.types.Operator):
	# indirectly bound
	bl_idname = "kc3dsbpy.deactivate_frame"
	bl_label = "Revert Frame"
	bl_description = "Tries to revert changes caused by Setup Frame"

	def invoke(self, context, event):
		GizmoContext(context.scene).deactivate()
		return {"FINISHED"}

def register():
	# All of these are hidden and internal!
	# Object
	bpy.types.Object.kc3dsbpy_gizmo_activated = bpy.props.BoolProperty(name = "Gizmo Activated", default = False)
	bpy.types.Object.kc3dsbpy_gizmo_hide_render_old = bpy.props.BoolProperty(name = "Gizmo Hide Render Backup", default = False)
	bpy.types.Object.kc3dsbpy_gizmo_hide_viewport_old = bpy.props.BoolProperty(name = "Gizmo Hide Viewport Backup", default = False)
	bpy.types.Object.kc3dsbpy_gizmo_ex_old = bpy.props.FloatProperty(name = "Gizmo EX Backup", default = 0)
	bpy.types.Object.kc3dsbpy_gizmo_ey_old = bpy.props.FloatProperty(name = "Gizmo EY Backup", default = 0)
	bpy.types.Object.kc3dsbpy_gizmo_ez_old = bpy.props.FloatProperty(name = "Gizmo EZ Backup", default = 0)
	bpy.types.Object.kc3dsbpy_gizmo_et_old = bpy.props.StringProperty(name = "Gizmo ET Backup", default = "XYZ")
	# Scene
	bpy.types.Scene.kc3dsbpy_gizmo_activated = bpy.props.BoolProperty(name = "Gizmo Activated", default = False)
	bpy.types.Scene.kc3dsbpy_gizmo_resx_old = bpy.props.IntProperty(name = "Gizmo ResX Backup", default = 512)
	bpy.types.Scene.kc3dsbpy_gizmo_resy_old = bpy.props.IntProperty(name = "Gizmo ResY Backup", default = 512)
	# Operators
	bpy.utils.register_class(DeactivateFKC3DSBPY)

def unregister():
	bpy.utils.unregister_class(DeactivateFKC3DSBPY)

