#!/usr/bin/env python3
# wastemon - Monitors for known signs of the Wasteland Glitch (i.e. room type changes) and can attempt to restore things to a saved copy.

# caosprox - CPX server reference implementation
# Written starting in 2022 by 20kdc
# To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
# You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

import sys
import libcpx
import time
import json

marker_classifier = "1 3 9822"

if len(sys.argv) != 2:
	print("wastemon MODE")
	print("MODEs:")
	print(" record : writes wastemon.json - an archive of expected room types")
	print(" monitor : watches for and corrects deviations from recorded data")
	print(" monjump : like monitor but jumps the camera to the affected room, pauses all creatures, enables the debug map view, and sticks a marker in there")
	print(" continue : unpauses everything and deletes the markers to undo monjump")
	print(" debugon : setv game \"engine_debug_keys\" 1 rgam")
	print("markers are of classifier " + marker_classifier)
	print("this tool is for experimentation ONLY and should not be used to keep a world with geats stable")
	print("invocation of bugs that corrupt memory may cause permanent damage to your creatures, world, scriptorium, and possibly more trouble outside of that")
elif sys.argv[1] == "record":
	rooms_str = libcpx.execute_caos_default("outs erid -1")
	rooms = {}
	for v in rooms_str.split(" "):
		if v != "":
			rooms[v] = libcpx.execute_caos_default("outv rtyp " + v)
	f = open("wastemon.json", "w")
	json.dump(rooms, f)
	f.close()
elif sys.argv[1] == "monitor" or sys.argv[1] == "monjump":
	jump = sys.argv[1] == "monjump"
	rooms = json.load(open("wastemon.json", "r"))
	while True:
		# scanning loop
		time.sleep(0.01)
		for k in rooms.keys():
			time.sleep(0.01)
			ctyp = libcpx.execute_caos_default("outv rtyp " + k)
			if ctyp != rooms[k]:
				# deviation!!!
				print("INCIDENT: room " + k + " is canonically " + rooms[k] + " but became " + ctyp + "!")
				libcpx.execute_caos_default("rtyp " + k + " " + rooms[k])
				if jump:
					# initial setup, stop creatures
					libcpx.execute_caos_default("enum 4 0 0 paus 1 next dmap 1")
					# where is room
					where = libcpx.execute_caos_default("outs rloc " + k).split(" ")
					x = (int(where[0]) + int(where[1])) >> 1
					y = (int(where[2]) + int(where[3]) + int(where[4]) + int(where[5])) >> 2
					# add marker (which we then pause)
					libcpx.execute_caos_default("new: simp " + marker_classifier + " \"fav_place_workshop\" 1 1 9997 mvsf " + str(x) + " " + str(y) + " paus 1")
					# move camera
					libcpx.execute_caos_default("cmrp " + str(x) + " " + str(y) + " 0")
elif sys.argv[1] == "continue":
	libcpx.execute_caos_default("enum " + marker_classifier + " kill targ next enum 0 0 0 paus 0 next dmap 0")
elif sys.argv[1] == "debugon":
	libcpx.execute_caos_default("setv game \"engine_debug_keys\" 1 rgam")
else:
	print("that's not a valid mode!")

