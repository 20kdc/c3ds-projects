#!/usr/bin/env python3
# Converts a DS195 linux build into a distribution of just the engine & ciesetup resources - essentially a "C2E engine distribution".
# Doing things this way will make more sense with the Advanced World Switcher project, assuming that ever happens...

# ciesetup - The ultimate workarounds to fix an ancient game
# Written starting in 2022 by contributors (see CREDITS.txt)
# To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
# You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

import sys
import os

# "change current directory to here" script
get_script_path = "SCRIPT_PATH=\"`readlink -e \"$0\"`\"\nSCRIPT_DIR=\"`dirname \"$SCRIPT_PATH\"`\"\n"
# get_script_path += "echo SP $SCRIPT_PATH SD $SCRIPT_DIR\n"
geocentric = get_script_path + "cd \"$SCRIPT_DIR\"\n"

gadgets_base = sys.argv[1]

os.mkdir("common_catalogues")

# - cleanup -

os.system("rm *.so*")

# Among other things, all the stuff that should go into the Docking Station archive
os.unlink("Readme.txt")
os.unlink("Readme-SDL.txt")
os.unlink("lc2e")
os.unlink("imageconvert")
os.unlink("SDLStretch.zip")
os.unlink("dockingstation")
os.unlink("dstation-install")
os.unlink("fr.xpm")
os.unlink("de.xpm")
os.unlink("en-GB.xpm")
os.unlink("langpick")
os.unlink("langpick.conf")
os.unlink("file_list.txt")
os.unlink("file_list_linux_x86_glibc21.txt")
os.unlink("dstation.xpm")
os.unlink("dstation.bmp")
os.unlink("Docking Station.ico")
os.unlink("machine.cfg")
os.unlink("user.cfg")
os.unlink("Porting-Credits.txt")
os.unlink("BuildNumber.txt")

# pull the DS version of the engine catalogues
os.system("mv \"Catalogue/voices.catalogue\" common_catalogues/")
os.system("mv \"Catalogue/vocab constructs.catalogue\" common_catalogues/")
os.system("mv Catalogue/System*.catalogue common_catalogues/")
os.system("mv Catalogue/Norn*.catalogue common_catalogues/")
os.system("mv Catalogue/CAOS.catalogue common_catalogues/")
os.system("mv Catalogue/Brain*.catalogue common_catalogues/")
os.system("mv Catalogue/NetBabel*.catalogue common_catalogues/")

# - directories to remove -

os.system("rm -rf \"Sounds\"")
os.system("rm -rf \"Overlay Data\"")
os.system("rm -rf \"My Worlds\"")
os.system("rm -rf \"My Agents\"")
os.system("rm -rf \"Images\"")
os.system("rm -rf \"Genetics\"")
os.system("rm -rf \"Catalogue\"")
os.system("rm -rf \"Bootstrap\"")
os.system("rm -rf \"Body Data\"")
os.system("rm -rf \"Backgrounds\"")

# link this back together
os.rename("common_catalogues", "Catalogue")
