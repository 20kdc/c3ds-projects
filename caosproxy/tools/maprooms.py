#!/usr/bin/env python3
# Maps rooms.

# caosprox - CPX server reference implementation
# Written starting in 2022 by 20kdc
# To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
# You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

import sys
import libcpx

if len(sys.argv) == 1:
	print("<!-- mapping all metarooms -->")
	metarooms_str = libcpx.execute_caos_default("outs emid")
elif len(sys.argv) == 2:
	metarooms_str = sys.argv[1]
	print("<!-- mapping metarooms " + metarooms_str + " -->")
else:
	raise Exception("either no args or one arg (space-separated metaroom ID list)")

def line(x1, y1, x2, y2):
	print("\t\t\t<path style=\"fill:none;stroke:#000000;stroke-width:4;\" d=\"m " + str(x1) + "," + str(y1) + " " + str(x2 - x1) + "," + str(y2 - y1) + "\"/>")

mapw = libcpx.execute_caos_default("outv mapw")
maph = libcpx.execute_caos_default("outv maph")
print("<svg width=\"" + mapw + "\" height=\"" + maph + "\">")
print("\t<rect width=\"" + mapw + "\" height=\"" + maph + "\" style=\"fill: white;\"/>")

for v in metarooms_str.split(" "):
	if v != "":
		print("\t<g id=\"metaroom" + v + "\">")
		# chart metaroom borders
		rect = libcpx.execute_caos_default("outs mloc " + v).split(" ")
		rect_x, rect_y, rect_w, rect_h = int(rect[0]), int(rect[1]), int(rect[2]), int(rect[3])
		print("\t\t<g id=\"metaroom" + v + "_border\">")
		line(rect_x, rect_y, rect_x + rect_w, rect_y)
		line(rect_x + rect_w, rect_y, rect_x + rect_w, rect_y + rect_h)
		line(rect_x + rect_w, rect_y + rect_h, rect_x, rect_y + rect_h)
		line(rect_x, rect_y + rect_h, rect_x, rect_y)
		print("\t\t</g>")

		rooms_str = libcpx.execute_caos_default("outs erid " + v)
		for v in rooms_str.split(" "):
			if v != "":
				print("\t\t<g id=\"metaroom" + v + "_room_" + v + "\">")
				# chart room borders
				poly = libcpx.execute_caos_default("outs rloc " + v).split(" ")
				poly_xl, poly_xr, poly_ytl, poly_ytr, poly_ybl, poly_ybr = int(poly[0]), int(poly[1]), int(poly[2]), int(poly[3]), int(poly[4]), int(poly[5])
				line(poly_xl, poly_ytl, poly_xr, poly_ytr)
				line(poly_xr, poly_ytr, poly_xr, poly_ybr)
				line(poly_xr, poly_ybr, poly_xl, poly_ybl)
				line(poly_xl, poly_ybl, poly_xl, poly_ytl)
				print("\t\t</g>")
		print("\t</g>")
print("</svg>")

