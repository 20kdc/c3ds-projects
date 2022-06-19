#!/usr/bin/env python3
# Given input and output TAR file names, decompresses .bz2 files, filename manipulation

# ciesetup - The ultimate workarounds to fix an ancient game
# Written starting in 2022 by 20kdc
# To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
# You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

import bz2
import tarfile
import sys
import io

src = tarfile.TarFile(sys.argv[1], "r")
dst = tarfile.TarFile(sys.argv[2], "w")

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
	# important bit here
	translated_name = translated_name.lower()
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

src.close()
dst.close()

