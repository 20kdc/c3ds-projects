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
import preplib

src = tarfile.TarFile(sys.argv[1], "r")
dst = tarfile.TarFile(sys.argv[2], "w")

lowercase_prefixes = [
	"./Sounds/",
	"./Backgrounds/",
	"./Overlay Data/",
	"./Body Data/",
	"./Images/"
]
# we need to be a bit more paranoid with C3
# people will be cobbling these together out of their personal installs
known_directories = [
	"./Backgrounds/",
	"./Body Data/",
	"./Bootstrap/",
	"./Catalogue/",
	"./Creature Galleries/",
	"./Genetics/",
	"./Images/",
	"./Journal/",
	"./My Agents/",
	"./My Creatures/",
	"./My Worlds/",
	"./Overlay Data/",
	"./Sounds/",
	"./Users/"
]

for member in src.getmembers():
	member_name_lower = member.name.lower()
	if member_name_lower.endswith(".dll"):
		continue
	if member_name_lower.endswith(".exe"):
		continue
	if member_name_lower.endswith(".map"):
		continue
	if member_name_lower.endswith(".url"):
		continue
	# we don't want engine catalogues, since we're using the engine package for that
	if member_name_lower.startswith("./catalogue/voices"):
		continue
	if member_name_lower.startswith("./catalogue/vocab constructs"):
		continue
	if member_name_lower.startswith("./catalogue/system"):
		continue
	if member_name_lower.startswith("./catalogue/norn"):
		continue
	if member_name_lower.startswith("./catalogue/caos"):
		continue
	if member_name_lower.startswith("./catalogue/brain"):
		continue
	if member_name_lower.startswith("./catalogue/netbabel"):
		continue
	# determine if and what to lowercase
	translated_name = member.name
	# start by fixing any potential errors in directory names
	for v in known_directories:
		if translated_name.lower().startswith(v.lower()):
			translated_name = v + translated_name[len(v):]
	# continue
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

def add_text_file(name, mode, text):
	data = text.encode("utf8")
	tarinfo = tarfile.TarInfo(name)
	tarinfo.mode = mode
	tarinfo.size = len(data)
	dst.addfile(tarinfo, io.BytesIO(data))

def add_dir(name):
	tarinfo = tarfile.TarInfo(name)
	tarinfo.mode = 0o755
	tarinfo.type = tarfile.DIRTYPE
	dst.addfile(tarinfo)

add_dir("./Creatures 3/Users")

machine_cfg_dm = preplib.DirectoryManager()
machine_cfg_dm.add_kv("Game Name", "Creatures 3")
machine_cfg_dm.add_all_dirs("")
machine_cfg_dm.add_dir("Catalogue", "../engine/Catalogue")

add_text_file("./Creatures 3/machine.cfg", 0o644, machine_cfg_dm.content)

# Yet another DS compatibility patch
add_text_file("./Creatures 3/Catalogue/aaa_ds_engine_compat.catalogue", 0o644, """
# added by prep_creatures3.py - support for DS engine
# If we don't have this, agent files won't load, period...
TAG "Pray System File Extensions"
"creature"
"agent"
"agents"
"blueprint"
"family"
"warp"
""")

add_text_file("./Creatures 3/user.cfg", 0o644, """
"Default Background" "c3_splash"
FullScreen 0
""")

src.close()
dst.close()

