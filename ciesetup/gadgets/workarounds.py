#!/usr/bin/env python3
# Installs the workarounds in the current directory.

# ciesetup - The ultimate workarounds to fix an ancient game
# Written starting in 2022 by 20kdc
# To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
# You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

import os

# "change current directory to here" script
geocentric = "\n# current directory = script directory\ncd $(dirname \"`readlink -e $0`\")\n"

# - cleanup -

# get rid of stuff that's only going to clutter things and that we can't really make good use of
os.unlink("dockingstation")
os.unlink("dstation-install")
os.unlink("fr.xpm")
os.unlink("de.xpm")
os.unlink("en-GB.xpm")
os.unlink("langpick")
os.unlink("langpick.conf")
os.unlink("file_list.txt")
os.unlink("file_list_linux_x86_glibc21.txt")
# imageconvert gets to live, not for any reasonable reason but because it could theoretically help with Mac stuff.
# langpick doesn't get to live - it hides languages because they aren't supported enough I guess
# so tell it to get outta here

# - machine.cfg -

# now then, machine.cfg C3 config, how do we deal with this?
# answer: we let symlinks deal with it and set things up so you can do that.
# seriously I'm not sure what the deal was with meticulously writing in the exact details of where C3 is into the config.
aux_names_a = ["Backgrounds", "Body Data", "Bootstrap", "Catalogue", "Creature Database", "Exported Creatures", "Genetics", "Images", "Journal", "Main", "Overlay Data", "Resource Files", "Sounds", "Users", "Worlds"]
aux_names_b = ["Backgrounds/", "Body Data/", "Bootstrap/", "Catalogue/", "Creature Galleries/", "My Creatures/", "Genetics/", "Images/", "Journal/", "", "Overlay Data/", "My Agents/", "Sounds/", "Users/", "My Worlds/"]

launcher_file = open("machine.cfg", "a")

launcher_file.write("\n")
for i in range(len(aux_names_a)):
	launcher_file.write("\"Auxiliary 1 " + aux_names_a[i] + " Directory\" \"Creatures 3/" + aux_names_b[i] + "\"\n")
launcher_file.close()

# - languages language.cfg dataset -

language_names = ["American English", "British English", "Deutsch", "Français", "Nederlands", "Italiano", "Español"]
language_codes = ["en", "en-GB", "de", "fr", "nl", "it", "es"]
language_cl = ["american", "english-uk", "deu", "fra", "nld", "ita", "esp"]

os.mkdir("languages")

for i in range(len(language_names)):
	nfile = open("languages/" + language_codes[i], "w")
	nfile.write("Language " + language_codes[i] + "\n")
	nfile.write("LanguageCLibrary " + language_cl[i] + "\n")
	nfile.close()

nfile = open("languages/list", "w")
for i in range(len(language_names)):
	nfile.write(language_codes[i] + "\n")
	nfile.write(language_names[i] + "\n")
nfile.close()

# - language selector -

launcher_file = open("select-language", "w")
launcher_file.write("#!/bin/sh\n")
launcher_file.write(geocentric)
launcher_file.write("zenity --list --column=\"Code\" --column=\"Language\" --hide-column 1 --text=\"Select the language in which to play this game.\\n(Only British English, French, and German appear in the normal language picker,\\nso support may be spotty for other languages.)\\nYou can change this later using the select-language script.\" < languages/list > languages/choice || exit 1\n")
launcher_file.write("cp \"languages/`cat languages/choice`\" language.cfg || exit 1\n")
launcher_file.close()

os.chmod("select-language", 0o755)

langcentric = "test -e language.cfg || ./select-language || exit 1\n"

# - simpler launcher script -

launcher_file = open("dockingstation", "w")
launcher_file.write("#!/bin/sh\n")
launcher_file.write(geocentric)
launcher_file.write(langcentric)
launcher_file.write("export LD_LIBRARY_PATH=.\n")
launcher_file.write("exec ./lc2e\n")
launcher_file.close()

os.chmod("dockingstation", 0o755)

# - missing empty directories -

empty_dirs = ["My Creatures", "Journal", "Creature Galleries", "Users"]
for v in empty_dirs:
	os.mkdir(v)
	open(v + "/.placeholder", "w").close()

# - user.cfg -

user_cfg = open("user.cfg", "r")
user_cfg_text = user_cfg.read()
user_cfg.close()
user_cfg_text = user_cfg_text.replace("DS_", "ds_")
user_cfg = open("user.cfg", "w")
user_cfg.write(user_cfg_text)
user_cfg.close()

# - creatures 3 dir, symlink, support infrastructure -

os.mkdir("Creatures 3")

launcher_file = open("Creatures 3/README.txt", "w")
launcher_file.write("You would put Creatures 3's files in this directory. Note this functionality of ciesetup isn't fully complete (the creatures3 script probably won't work)\n")
launcher_file.close()

launcher_file = open("creatures3", "w")
launcher_file.write("#!/bin/sh\n")
launcher_file.write(geocentric)
launcher_file.write(langcentric)
launcher_file.write("# we're running this on the DS engine because we have cool sunglasses\n")
launcher_file.write("# but that means we need to borrow a file from DS\n")
launcher_file.write("if [ ! -e \"Creatures 3/Catalogue/vocab constructs.catalogue\" ]; then\n")
launcher_file.write("\tmkdir -p \"Creatures 3/Catalogue\"\n")
launcher_file.write("\tcp \"Catalogue/vocab constructs.catalogue\" \"Creatures 3/Catalogue/vocab constructs.catalogue\"\n")
launcher_file.write("fi\n")
launcher_file.write("# continue forth\n")
launcher_file.write("cd \"Creatures 3\"\n")
launcher_file.write("export LD_LIBRARY_PATH=..\n")
launcher_file.write("exec ../lc2e\n")
launcher_file.close()

os.chmod("creatures3", 0o755)


