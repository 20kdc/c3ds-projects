#!/usr/bin/env python3
# Decomplicates things, I promise.

# libkc3ds - General-purpose Python library for C3/DS
# Written starting in 2023 by contributors (see CREDITS.txt)
# To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
# You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

import bpy
import mathutils
import math

# See VISSCRIPT.md

VISSCRIPT_OPS = ["|", "&", "!", "="]

def visscript_compile_op(l, op, r):
	if op == "|":
		ls = visscript_compile(l)
		rs = visscript_compile(r)
		return lambda props: ls(props) or rs(props)
	elif op == "&":
		ls = visscript_compile(l)
		rs = visscript_compile(r)
		return lambda props: ls(props) and rs(props)
	elif op == "=":
		prop = l.strip()
		val = r.strip()
		return lambda props: (prop in props) and (str(props[prop]) == val)
	elif op == "!":
		if l != "":
			raise Exception("cannot use ! after something")
		rs = visscript_compile(r)
		return lambda props: rs(props)
	else:
		# shouldn't even be possible
		raise Exception("Unknown op: " + op)

def visscript_truthy(val):
	val = str(val)
	if val == "":
		return False
	elif val == "0":
		return False
	return True

def visscript_compile(script):
	script = script.strip()
	# empty?
	if script == "":
		# 'Set' component (lighting cams etc.) : always valid
		return lambda props: True
	# try to find op
	for op in VISSCRIPT_OPS:
		op_idx = script.find(op)
		if op_idx != -1:
			return visscript_compile_op(script[:op_idx], op, script[op_idx + len(op):])
	# flag: prop is present and non-zero
	prop = script
	return lambda props: (prop in props) and visscript_truthy(str(props[prop]))

def visscript_compile_and_bind(obj):
	"""
	Creates a lambda which updates the object rendering status from Gizmo properties.
	"""
	compiled = visscript_compile(obj.kc3dsbpy_visscript)
	def bound(props):
		obj.hide_render = not compiled(props)
		obj.hide_viewport = obj.hide_render
	return bound

class GizmoContext():
	"""
	A scan of the scene, etc.
	"""
	def __init__(self, scene):
		self.scene = scene
		self.camera = scene.camera
		self.markers = {}
		if self.camera is None:
			raise Exception("Camera required!")
		self.vis = []
		for obj in scene.objects:
			mk = obj.kc3dsbpy_part_marker
			if mk != "":
				if mk in self.markers:
					raise Exception("Duplicate marker: " + self.markers[mk].name + " to " + obj.name)
				self.markers[mk] = obj
			self.vis.append(visscript_compile_and_bind(obj))

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
		# Camera is kept away from model using parenting
		# So Gizmo is deliberately kept not aware of it
		# Would be nice if we had global marker location, but what can 'ya do
		self.camera.location = marker.location
		# Setup visibility.
		for vis in self.vis:
			vis(props)

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
				obj.hide_render = obj.kc3dsbpy_gizmo_hide_render_old
				obj.hide_viewport = obj.kc3dsbpy_gizmo_hide_viewport_old
				obj.kc3dsbpy_gizmo_activated = False
				obj.rotation_euler = mathutils.Euler((obj.kc3dsbpy_gizmo_ex_old, obj.kc3dsbpy_gizmo_ey_old, obj.kc3dsbpy_gizmo_ez_old), obj.kc3dsbpy_gizmo_et_old)

def register():
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

