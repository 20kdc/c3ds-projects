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
	# try to find op
	for op in VISSCRIPT_OPS:
		op_idx = script.index(op)
		if op_idx != -1:
			return visscript_compile_op(script[:op_idx], op, script[op_idx + len(op):])
	# flag: prop is present and non-zero
	prop = script
	return lambda props: (prop in props) and visscript_truthy(str(props[prop]))

class Gizmo():
	"""
	Manipulation of objects made easy.
	"""
	def __init__(self, context, props):
		self.context = context
		self.props = props
		if not "SelfieArm" in bpy.data.objects:
			raise Exception("There needs to be an Empty called SelfieArm. This is moved about in order to position the camera, so the camera should be parented to it.")
		self.selfie_arm = bpy.data.objects["SelfieArm"]
		if not "RefCamera" in bpy.data.objects:
			raise Exception("There needs to be a Camera called RefCamera. This is the reference camera from which data is copied.")
		self.cam_ref = bpy.data.objects["RefCamera"]
		if not "ActCamera" in bpy.data.objects:
			raise Exception("There needs to be a Camera called ActCamera. This is the target camera used for real renders (and thus gets modified!).")
		self.cam_act = bpy.data.objects["ActCamera"]
		self.part_name = props["part_name"]
		if not self.part_name in bpy.data.objects:
			raise Exception("There needs to be some object called " + self.part_name + ". This is a targetted part. The location of this object will be used for the camera, any objects with names beginning with " + self.part_name + " will be rendered for this part.")
		else:
			self.target = bpy.data.objects[self.part_name]

	def activate(self):
		# step 1. frame
		self.frame_old = self.context.scene.frame_current
		# step 2. camera
		self.res_x_old = self.context.scene.render.resolution_x
		self.res_y_old = self.context.scene.render.resolution_y
		self.context.scene.render.resolution_x = self.props["size"]
		self.context.scene.render.resolution_y = self.props["size"]
		self.context.scene.camera = self.cam_act
		# 100 is used as it's the value for a full adult head, so things properly cancel out.
		# I am quite aware this is ugly and I may be eaten by a grue on some dark, stormy night.
		# Scale is put on the division end because a smaller target part scale needs to mean a higher ortho scale.
		# Could actually scale the part, but I think that would have adverse effects, and it would also break the test model r/n. Bad idea.
		self.cam_act.data.ortho_scale = self.cam_ref.data.ortho_scale * self.props["size"] / (100.0 * self.props["scale"])
		# step 3. position selfie arm
		self.selfie_arm.location = self.target.location
		# step 4. adjust hiddenness
		self.hidden = []
		for v in bpy.data.objects:
			if (not v.name.startswith(self.part_name)) and not (v.name.startswith("SET.")):
				if not v.hide_render:
					v.hide_render = True
					self.hidden.append(v)
		# step 5. adjust target rotation
		self.rotation_backup = self.target.rotation_euler
		self.target.rotation_euler = mathutils.Euler((math.radians(self.props["pitch"]), 0, math.radians(self.props["yaw"])), "XYZ")

	def deactivate(self):
		# step 5. adjust target rotation
		self.target.rotation_euler = mathutils.Euler((0, 0, 0), "XYZ")
		# step 4. adjust hiddenness
		for v in self.hidden:
			v.hide_render = False
		# step 3. position selfie arm
		self.selfie_arm.location = mathutils.Vector((0, 0, 0))
		# step 2. camera
		self.context.scene.camera = self.cam_ref
		self.context.scene.render.resolution_x = self.res_x_old
		self.context.scene.render.resolution_y = self.res_y_old
		# step 1. frame
		self.context.scene.frame_current = self.frame_old

