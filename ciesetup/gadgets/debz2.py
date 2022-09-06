#!/usr/bin/env python3
# Given input and output TAR file names, decompresses .bz2 files, filename manipulation
# The idea here is to produce a "close enough" tree to that created by the installer.
# We can muck with things later, but we shouldn't be outright patching things in this step!

# ciesetup - The ultimate workarounds to fix an ancient game
# Written starting in 2022 by contributors (see CREDITS.txt)
# To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
# You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

import bz2
import tarfile
import sys
import io
import time

src = tarfile.TarFile(sys.argv[1], "r")
dst = tarfile.TarFile(sys.argv[2], "w")

lowercase_prefixes = ["./Sounds/", "./Backgrounds/", "./Overlay Data", "./Body Data/", "./Images/"]

for member in src.getmembers():
	# all the files we care about end in .bz2
	if not member.name.endswith(".bz2"):
		continue
	member_name_nbz2 = member.name[:-4]
	# determine what this is and if we care
	translated_name = None
	if member_name_nbz2.startswith("./ports/linux_x86_glibc21_64/"):
		translated_name = "./" + member_name_nbz2[29:]
	elif member.name.startswith("./dsbuild 195/global/"):
		translated_name = "./" + member_name_nbz2[21:]
	if translated_name is None:
		continue
	# determine if and what to lowercase
	for v in lowercase_prefixes:
		if translated_name.startswith(v):
			translated_name = v + (translated_name[len(v):].lower())
	# alright, extract it
	f = src.extractfile(member)
	data = f.read()
	f.close()
	data = bz2.decompress(data)
	# store
	tarinfo = tarfile.TarInfo(translated_name)
	tarinfo.mtime = member.mtime
	tarinfo.mode = member.mode
	tarinfo.size = len(data)
	dst.addfile(tarinfo, io.BytesIO(data))

# This is really just to preserve the illusion that this is a complete install.
# There's an issue with dstation-install that requires this be an absolute symlink.
# In the interest of making a fair attempt at making this part actually usable if you want the "authentic" layout,
#  I've put the default install directory here.

tarinfo = tarfile.TarInfo("./dockingstation")
tarinfo.type = tarfile.SYMTYPE
tarinfo.mtime = time.time()
tarinfo.linkname = "/usr/local/games/dockingstation/dstation-install"
tarinfo.size = len(data)
dst.addfile(tarinfo)

src.close()
dst.close()

