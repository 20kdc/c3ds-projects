#!/usr/bin/env python3
# frame request calculator

# libkc3ds - General-purpose Python library for C3/DS
# Written starting in 2023 by contributors (see CREDITS.txt)
# To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
# You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

import os

import bpy
from bpy.types import Operator

import libkc3ds.parts
import libkc3ds.aging
import libkc3ds.s16
from libkc3ds.att import ATTFile

from . import gizmo
from . import imaging

def calc_pitch_auto(marker, pid):
	"""
	Calculates automatic pitch. Manual pitch is ignored.
	(Also I am aware that the + on the trim should probably be a - ; it's a compat thing now)
	"""
	return (pid * -22.5 * marker.kc3dsbpy_pitch_mul) + marker.kc3dsbpy_pitch_trim

def calc_pitch_mul(fm1, f2):
	"""
	Reverses manual to automatic pitch.
	"""
	fm1_trim_removed = fm1 - calc_pitch_trim(fm1, f2)
	return fm1_trim_removed / 22.5 # 22.5 = (pid:-1) * -22.5

def calc_pitch_trim(fm1, f2):
	"""
	Reverses manual to automatic pitch.
	"""
	return (fm1 * 0.666666666666) + (f2 * 0.333333333333)

def calc_pitch(marker, pid, yid):
	"""
	Calculates pitch.
	"""
	if not marker.kc3dsbpy_pitch_manual:
		return calc_pitch_auto(marker, pid)
	sides = yid == -1 or yid == 1
	if pid == -1:
		if sides:
			return marker.kc3dsbpy_pitch_sm1
		return marker.kc3dsbpy_pitch_fm1
	if pid == 0:
		if sides:
			return marker.kc3dsbpy_pitch_s0
		return marker.kc3dsbpy_pitch_f0
	if pid == 1:
		if sides:
			return marker.kc3dsbpy_pitch_s1
		return marker.kc3dsbpy_pitch_f1
	if sides:
		return marker.kc3dsbpy_pitch_s2
	return marker.kc3dsbpy_pitch_f2

# Skeleton Request Calculator

class SkeletonReqContext():
	"""
	Contains information collated for a skeleton.
	"""
	def __init__(self, gizmo_context, cset, sex, age_char):
		scene = gizmo_context.scene
		self.setup = cset.setup
		self.gizmo_context = gizmo_context
		self.path_gb = os.path.join(scene.kc3dsbpy_render_genus, scene.kc3dsbpy_render_breed, sex, age_char)
		self.age_data = cset.ages[age_char]
		self.pixels_per_unit = scene.kc3dsbpy_render_ppu
		gs_char = libkc3ds.parts.C3_GS_MAP[sex + scene.kc3dsbpy_render_genus]
		self.props = {
			"genus": scene.kc3dsbpy_render_genus,
			"breed": scene.kc3dsbpy_render_breed,
			"breed_num": ord(scene.kc3dsbpy_render_breed) - 97,
			"gs_num": int(gs_char),
			"male": 0,
			"female": 0,
			"age": int(age_char),
			"mode": scene.kc3dsbpy_render_mode
		}
		self.props[sex] = 1
		# Last 3 characters as per QuickNorn.
		self.xyz = gs_char + age_char + scene.kc3dsbpy_render_breed

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
		part_info = self.setup.part_names_to_infos[part_name]
		# calculate file paths
		cv = os.path.join(self.path_gb, "CA%04d" % new_props["frame"])
		path_png = cv + ".png"
		path_bmp = cv + ".bmp"
		part_char = self.setup.part_names_to_infos[part_name].char
		fsb = part_char + self.xyz
		paths = ReqPaths(path_png, path_bmp, fsb, new_props["frame_rel"])
		# check part name exists as a marker, if not, we'll have to skip
		if not (part_name in self.gizmo_context.markers):
			return BlankReq(part_info, paths)
		# determine inheritance
		marker = self.gizmo_context.markers[part_name]
		marker_o = marker
		if not (marker.kc3dsbpy_marker_inherit is None):
			marker = marker.kc3dsbpy_marker_inherit
			# stop infinite loops the easy way
			if not (marker.kc3dsbpy_marker_inherit is None):
				raise Exception("Marker " + marker_o.name + " inherits from " + marker.name + " which also inherits, this is not allowed")
		# infuse part ASCII
		new_props["part_ascii"] = ord(part_char)
		# infuse age data
		aged_part = self.age_data.parts[new_props["part"]]
		new_props["width"] = aged_part.size
		new_props["height"] = aged_part.size
		new_props["ortho_scale"] = aged_part.size / (self.age_data.scale * self.pixels_per_unit * marker.kc3dsbpy_ppu_factor)
		# infuse rotation data
		new_props["pitch"] = calc_pitch(marker, new_props["pitch_id"], new_props["yaw_id"])
		new_props["yaw"] = new_props["yaw_id"] * 90
		new_props["roll"] = 0
		return FrameReq(self.gizmo_context, new_props, part_info, paths)

class ReqPaths():
	"""
	Describes file paths.
	"""
	def __init__(self, path_png, path_bmp, fsb, frame_c16):
		self.png = path_png
		self.bmp = path_bmp
		# i.e. "a00a"
		self.fsb = fsb
		self.c16_frame = frame_c16

	def __str__(self):
		return self.png + " (" + self.fsb + "/" + str(self.c16_frame) + ")"

class BlankReq():
	"""
	Describes a blank frame request.
	"""
	def __init__(self, part_info, paths):
		self.part_name = part_info.part_id.name
		self.part_info = part_info
		self.paths = paths

class FrameReq(BlankReq):
	"""
	Describes a single frame request.
	Frame requests must be in C16 order so they collate properly.
	"""
	def __init__(self, gizmo_context, gizmo_props, part_info, paths):
		super().__init__(part_info, paths)
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

CSETS = {}
for cset in libkc3ds.aging.CSETS_ALL:
	CSETS[cset.name] = cset

def scene_to_cset(scene):
	if not scene.kc3dsbpy_cset in CSETS:
		return libkc3ds.aging.CHICHI
	return CSETS[scene.kc3dsbpy_cset]

def calc_req_group(scene):
	"""
	Calculates an all-frame operation requested by the given Scene.
	"""
	gizmo_context = gizmo.GizmoContext(scene)
	cset = scene_to_cset(scene)
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
	cset = scene_to_cset(scene)
	frc = SkeletonReqContext(gizmo_context, cset, scene.kc3dsbpy_render_sex, scene.kc3dsbpy_render_age)
	return frc.frame_req(cset.setup.frames[scene.kc3dsbpy_render_frame])

# Operators

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
		framereqs = calc_req_group(scene)
		gizmo_idx = 0
		# Main rendering phase
		for frame in framereqs:
			print("GIZMOBATCH: " + str(gizmo_idx) + " / " + str(len(framereqs)))
			path_png = os.path.join(path_base, frame.paths.png)
			scene.render.image_settings.file_format = "PNG"
			scene.render.image_settings.color_mode = "RGBA"
			out_image = padding_frame
			if type(frame) == FrameReq:
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

class CompileATTsKC3DSBPY(Operator):
	# indirectly bound
	bl_idname = "kc3dsbpy.compile_body_data"
	bl_label = "Compile Body Data From Rig"
	bl_description = "Compiles breed body data from Empties (Requires that you have these setup)"

	def invoke(self, context, event):
		scene = context.scene
		# actually prepare
		path_base = bpy.path.abspath(scene.kc3dsbpy_att_outpath)
		framereqs = calc_req_group(scene)
		gizmo_idx = 0
		# Main phase
		# indexed by FSB
		att_collated = {}
		for frame in framereqs:
			part_info = frame.part_info
			if type(frame) == FrameReq:
				frame.activate()
				# prep ATT table...
				if not frame.paths.fsb in att_collated:
					att_collated[frame.paths.fsb] = ATTFile(part_info.setup.att_frames, len(part_info.att_point_names))
				att = att_collated[frame.paths.fsb]
				# get ATT data...
				for i in range(att.frames):
					# attention! this is what binds C16 frames to ATT frames!
					if frame.paths.c16_frame == i:
						for j in range(att.points):
							ptt = frame.gizmo_context.get_att(context, frame.part_name, j)
							att.set(i, j, ptt[0], ptt[1])
				# done!
				frame.deactivate()
			gizmo_idx += 1
		# Saving phase
		try:
			os.makedirs(path_base)
		except:
			pass
		for fsb in att_collated:
			f = open(os.path.join(path_base, fsb + ".att"), "wb")
			f.write(att_collated[fsb].encode())
			f.close()
		# Done!
		self.report({"INFO"}, "Completed ATT build")
		return {"FINISHED"}

def _iterate_framelists(scene, cb):
	# actually prepare
	path_ib = bpy.path.abspath(scene.render.filepath)
	framereqs = calc_req_group(scene)
	# actually do the thing
	fsb_names = {}
	for frame in framereqs:
		fsb_names[frame.paths.fsb] = True
	for fsb in fsb_names:
		print(fsb)
		relevant_frames = []
		for frame in framereqs:
			if frame.paths.fsb == fsb:
				relevant_frames.append(frame)
		def inner(cb):
			# load and dither
			for frame in relevant_frames:
				path_png = os.path.join(path_ib, frame.paths.png)
				tmp_img = bpy.data.images.load(path_png)
				cb(tmp_img)
				bpy.data.images.remove(tmp_img)
		cb(fsb + ".c16", inner, relevant_frames)

def _iterate_framelists_c16ify(scene, inner):
	# dithering modes
	if scene.kc3dsbpy_c16_dither_colour:
		cdmode = "bayer2"
	else:
		cdmode = "floor"
	if scene.kc3dsbpy_c16_dither_alpha:
		admode = "bayer2"
	else:
		admode = "nearest"
	# the core
	c16_frames = []
	def y(tmp_img):
		c16_frames.append(imaging.bpy_to_s16image(tmp_img, cdmode = cdmode, admode = admode))
	inner(y)
	return c16_frames

class PNG2C16KC3DSBPY(Operator):
	# indirectly bound
	bl_idname = "kc3dsbpy.png2c16"
	bl_label = "PNG -> C16"
	bl_description = "Converts PNGs to C16s. Uses the rendering configuration for inputs"

	def invoke(self, context, event):
		scene = context.scene
		# actually prepare
		path_cb = bpy.path.abspath(scene.kc3dsbpy_c16_outpath)
		state = {"files_written": 0}
		def x(c16, inner, relevant_frames):
			c16_frames = _iterate_framelists_c16ify(scene, inner)
			# finish
			imaging.save_c16_with_makedirs(c16_frames, os.path.join(path_cb, c16))
			state["files_written"] += 1
		_iterate_framelists(scene, x)
		self.report({"INFO"}, "Completed PNG->C16, " + str(state["files_written"]) + " files written")
		return {"FINISHED"}

class MakeSheetsKC3DSBPY(Operator):
	# indirectly bound
	bl_idname = "kc3dsbpy.make_sheets"
	bl_label = "To Sheets"
	bl_description = "Converts PNGs to BMP sheets. Uses the rendering configuration for inputs"

	def invoke(self, context, event):
		scene = context.scene
		# actually prepare
		path_cb = bpy.path.abspath(scene.kc3dsbpy_c16_outpath)
		columns = 16
		def x(c16, inner, relevant_frames):
			c16_frames = _iterate_framelists_c16ify(scene, inner)
			# finish
			imaging.save_png_with_makedirs(imaging.make_sheet(c16_frames, columns), os.path.join(path_cb, c16 + ".png"))
		_iterate_framelists(scene, x)
		self.report({"INFO"}, "Completed sheets")
		return {"FINISHED"}

def activate_frame_op(operator, scene):
	frame = calc_req_frame(scene)
	if type(frame) == FrameReq:
		frame.activate()
		operator.report({"INFO"}, "Viewing: " + str(frame.paths))
	else:
		operator.report({"WARNING"}, "Missing marker '" + frame.part_name + "' @ " + str(frame.paths))

class ActivateFKC3DSBPY(Operator):
	# indirectly bound
	bl_idname = "kc3dsbpy.activate_frame"
	bl_label = "Setup Frame"
	bl_description = "Sets up a specific frame of breed rendering. Useful for troubleshooting"

	def invoke(self, context, event):
		activate_frame_op(self, context.scene)
		return {"FINISHED"}

# Registration

def register():
	bpy.utils.register_class(RenderKC3DSBPY)
	bpy.utils.register_class(CompileATTsKC3DSBPY)
	bpy.utils.register_class(PNG2C16KC3DSBPY)
	bpy.utils.register_class(MakeSheetsKC3DSBPY)
	bpy.utils.register_class(ActivateFKC3DSBPY)

def unregister():
	bpy.utils.unregister_class(RenderKC3DSBPY)
	bpy.utils.unregister_class(CompileATTsKC3DSBPY)
	bpy.utils.unregister_class(PNG2C16KC3DSBPY)
	bpy.utils.unregister_class(MakeSheetsKC3DSBPY)
	bpy.utils.unregister_class(ActivateFKC3DSBPY)

