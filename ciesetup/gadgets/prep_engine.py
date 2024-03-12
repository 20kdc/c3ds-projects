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

os.mkdir("engine")
os.mkdir("engine/Catalogue")

# - library stuff -

os.system("cp " + gadgets_base + "dummy.so ./engine/")
os.system("cp " + gadgets_base + "runtime.so ./engine/")
os.system("cp " + gadgets_base + "runtime_headless.so ./engine/")
os.system("ln -s dummy.so engine/libgtk-1.2.so.0")
os.system("ln -s dummy.so engine/libgdk-1.2.so.0")
os.system("ln -s dummy.so engine/libglib-1.2.so.0")
os.system("ln -s dummy.so engine/libgmodule-1.2.so.0")

# - cleanup -

os.system("mv *.so* engine/")
os.system("mv lc2e engine/")
os.system("mv Readme.txt engine/")
os.system("mv Readme-SDL.txt engine/")
os.system("mv SDLStretch.zip engine/")

# Among other things, all the stuff that should go into the Docking Station archive
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
os.system("mv \"Catalogue/voices.catalogue\" engine/Catalogue/")
os.system("mv \"Catalogue/vocab constructs.catalogue\" engine/Catalogue/")
os.system("mv Catalogue/System*.catalogue engine/Catalogue/")
os.system("mv Catalogue/Norn*.catalogue engine/Catalogue/")
os.system("mv Catalogue/CAOS.catalogue engine/Catalogue/")
os.system("mv Catalogue/Brain*.catalogue engine/Catalogue/")
os.system("mv Catalogue/NetBabel*.catalogue engine/Catalogue/")

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

# - moved executables -

os.system("mv imageconvert engine/")

# - languages language.cfg dataset -

language_names = ["American English", "British English", "Deutsch", "Français", "Nederlands", "Italiano", "Español"]
language_codes = ["en", "en-GB", "de", "fr", "nl", "it", "es"]
language_cl = ["american", "english-uk", "deu", "fra", "nld", "ita", "esp"]

for i in range(len(language_names)):
	nfile = open("engine/language-" + language_codes[i] + ".cfg", "w")
	nfile.write("Language " + language_codes[i] + "\n")
	nfile.write("LanguageCLibrary " + language_cl[i] + "\n")
	nfile.close()

nfile = open("engine/languages", "w")
for i in range(len(language_names)):
	nfile.write(language_codes[i] + "\n")
	nfile.write(language_names[i] + "\n")
nfile.close()

# - launch scripts -

# Language selector

launcher_file = open("engine/select-language", "w")
launcher_file.write("#!/bin/sh\n")
launcher_file.write(geocentric)
launcher_file.write("zenity --list --column=\"Code\" --column=\"Language\" --hide-column 1 --text=\"Select the language in which to play this game.\\n(Only British English, French, and German appear in the normal language picker,\\nso support may be spotty for other languages.)\\nYou can change this later using the select-language script.\" < languages > language || exit 1\n")
launcher_file.write("cp \"language-`cat language`.cfg\" language.cfg || exit 1\n")
launcher_file.close()
os.chmod("engine/select-language", 0o755)

os.system("ln -s engine/select-language select-language")

# Just so you know!

launcher_file = open("engine/error", "w")
launcher_file.write("#!/bin/sh\n")
launcher_file.write("echo ERROR\n")
launcher_file.write("echo \"$1\"\n")
launcher_file.write("zenity --error --text \"$1\"\n")
launcher_file.write("exit 1 # ensure this still errors for || chaining\n")
launcher_file.close()
os.chmod("engine/error", 0o755)

# Main game runner script

os.system("cp " + gadgets_base + "run-game ./engine/")

# Fancy aliases

launcher_file = open("dockingstation", "w")
launcher_file.write("#!/bin/sh\n")
launcher_file.write(geocentric)
launcher_file.write("cd \"Docking Station\" || engine/error \"The Docking Station directory does not exist or is inaccessible.\" || exit 1\n")
launcher_file.write("exec ../engine/run-game \"$@\"")
launcher_file.close()
os.chmod("dockingstation", 0o755)

launcher_file = open("creatures3", "w")
launcher_file.write("#!/bin/sh\n")
launcher_file.write(geocentric)
launcher_file.write("cd \"Creatures 3\" || engine/error \"The Creatures 3 directory does not exist or is inaccessible.\" || exit 1\n")
launcher_file.write("exec ../engine/run-game \"$@\"")
launcher_file.close()
os.chmod("creatures3", 0o755)

# Placeholders

os.mkdir("Docking Station")
launcher_file = open("Docking Station/README.txt", "w")
launcher_file.write("You would put Docking Station's files in this directory - a file containing them should have been generated with the engine distribution.\n")
launcher_file.close()

os.mkdir("Creatures 3")
launcher_file = open("Creatures 3/README.txt", "w")
launcher_file.write("You would put Creatures 3's files in this directory - use 'make c3' in the ciesetup directory if you need to convert from a Windows build.\n")
launcher_file.write("The creatures3 script will automatically install necessary compatibility files for using the Docking Station engine with standalone Creatures 3.\n")
launcher_file.close()


