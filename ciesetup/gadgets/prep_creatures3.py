#!/usr/bin/env python3
# Given input and output TAR file names, performs the conversion from a Windows Creatures 3 Update 2 tree to the packaged format

# ciesetup - The ultimate workarounds to fix an ancient game
# Written starting in 2022 by contributors (see CREDITS.txt)
# To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
# You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

import tarfile
import sys
import io
import time

src = tarfile.TarFile(sys.argv[1], "r")
dst = tarfile.TarFile(sys.argv[2], "w")

lowercase_prefixes = ["./Sounds/", "./Backgrounds/", "./Overlay Data", "./Body Data/", "./Images/"]

for member in src.getmembers():
	if member.name.lower().endswith(".dll"):
		continue
	if member.name.lower().endswith(".exe"):
		continue
	if member.name.lower().endswith(".map"):
		continue
	if member.name.lower().endswith(".url"):
		continue
	# determine if and what to lowercase
	translated_name = member.name
	for v in lowercase_prefixes:
		if translated_name.startswith(v):
			translated_name = v + (translated_name[len(v):].lower())
	translated_name = "./Creatures 3/" + translated_name[2:]
	# and in any case, what is this anyway?
	if member.isfile():
		# alright, extract it
		f = src.extractfile(member)
		data = f.read()
		f.close()
		# store
		tarinfo = tarfile.TarInfo(translated_name)
		tarinfo.mtime = member.mtime
		tarinfo.mode = member.mode
		tarinfo.size = len(data)
		dst.addfile(tarinfo, io.BytesIO(data))
	else:
		# add directly (ish)
		tarinfo = tarfile.TarInfo(translated_name)
		tarinfo.mtime = member.mtime
		tarinfo.mode = member.mode
		tarinfo.type = member.type
		dst.addfile(tarinfo)

src.close()
dst.close()
