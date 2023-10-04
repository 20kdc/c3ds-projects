#!/usr/bin/env python3
# ATT file writer.

# libkc3ds - General-purpose Python library for C3/DS
# Written starting in 2023 by contributors (see CREDITS.txt)
# To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
# You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

class ATTFile():
	"""
	ATT file data, wrapped up.
	"""
	def __init__(self, frames, points):
		self.frames = frames
		self.points = points
		self.x = []
		self.y = []
		for i in range(frames):
			self.x.append([0] * points)
			self.y.append([0] * points)
	def set(self, frame, point, x, y):
		self.x[frame][point] = x
		self.y[frame][point] = y
	def encode(self):
		"""
		Returns ATT file data as bytes.
		"""
		data = b""
		for i in range(self.frames):
			for j in range(self.points):
				if j != 0:
					data += b" "
				text = str(self.x[i][j]) + " " + str(self.y[i][j])
				data += text.encode("utf8")
			data += b"\n"
		return data

