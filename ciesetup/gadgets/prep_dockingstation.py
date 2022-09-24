#!/usr/bin/env python3
# Prepares the docking station sub-package (to be installed on an engine package) from the Docking Station tree

# ciesetup - The ultimate workarounds to fix an ancient game
# Written starting in 2022 by contributors (see CREDITS.txt)
# To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
# You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

import sys
import os

gadgets_base = sys.argv[1]

# Get rid of engine stuff and the like
os.unlink("dockingstation")
os.unlink("dstation-install")
os.unlink("fr.xpm")
os.unlink("de.xpm")
os.unlink("en-GB.xpm")
os.unlink("langpick")
os.unlink("langpick.conf")
os.unlink("file_list.txt")
os.unlink("file_list_linux_x86_glibc21.txt")
os.unlink("imageconvert")
os.unlink("lc2e")
os.unlink("lc2elib.so")
os.unlink("lc2e-netbabel.so")
os.unlink("libSDL-1.2.so.0")
os.unlink("libSDL_mixer-1.2.so.0")
os.unlink("libSDLStretch.so")
os.unlink("libstdc++-libc6.1-2.so.3")
os.unlink("SDLStretch.zip")

# delete DS version of engine catalogues because engine grabs them
os.system("rm Catalogue/voices.catalogue")
os.system("rm \"Catalogue/vocab constructs.catalogue\"")
os.system("rm Catalogue/System*.catalogue")
os.system("rm Catalogue/Norn*.catalogue")
os.system("rm Catalogue/CAOS.catalogue")
os.system("rm Catalogue/Brain*.catalogue")
os.system("rm Catalogue/NetBabel*.catalogue")

# - machine.cfg -

# now then, machine.cfg C3 config, how do we deal with this?
# answer: we let symlinks deal with it and set things up so you can do that.
# seriously I'm not sure what the deal was with meticulously writing in the exact details of where C3 is into the config.
aux_names_a = ["Backgrounds", "Body Data", "Bootstrap", "Catalogue", "Creature Database", "Exported Creatures", "Genetics", "Images", "Journal", "Main", "Overlay Data", "Resource Files", "Sounds", "Users", "Worlds"]
aux_names_b = ["Backgrounds/", "Body Data/", "Bootstrap/", "Catalogue/", "Creature Galleries/", "My Creatures/", "Genetics/", "Images/", "Journal/", "", "Overlay Data/", "My Agents/", "Sounds/", "Users/", "My Worlds/"]

launcher_file = open("machine.cfg", "a")

launcher_file.write("\n")
# add C3 aux.
for i in range(len(aux_names_a)):
	launcher_file.write("\"Auxiliary 1 " + aux_names_a[i] + " Directory\" \"../Creatures 3/" + aux_names_b[i] + "\"\n")
# add engine catalogue aux.
launcher_file.write("\"Auxiliary 2 Catalogue Directory\" \"../engine/Catalogue/\"\n")
launcher_file.close()

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

