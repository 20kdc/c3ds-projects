* `database.py`: Defines classes to bind together Setup, ages, and image sizes, along with ChiChi Norn metrics
* `gizmo.py`: VisScript, camera positioning, and scene manipulation runtime
* `dataext.py`: UI, defines properties used by init, responsible for constructing Gizmo property sets
* `__init__.py`: and outer render functions

## PROPS EMITTED BY LIBKC3DS

* `"part"`: Part name.
* `"frame"`: Frame number.
* `"frame_rel"`: Frame number, relative to part (C16 frame number).
* `"pitch_id"`: Pitch ID. Has a relationship to actual pitch.
* `"yaw_id"`: Yaw ID. Has a relationship to actual yaw.
* `"blank"`: 1 for "Face" part 0 otherwise
* `"expr"`: normal/happy/sad/angry/scared/sleepy
* `"normal"`/`"happy"`/`"sad"`/`"angry"`/`"scared"`/`"sleepy"`: 0/1 proxies for expr
* `"egg"`: 0-3
* `"eyes"`: 0/1

## PROPS READ BY GIZMO

All of these critical props are infused by `dataext.py`, except for `"part"` which comes from libkc3ds.

* `"part"`: Part name / marker.
* `"pitch"`, `"yaw"`, `"roll"`: Marker rotation in degrees.
* `"width"`: Camera width.
* `"height"`: Camera height.
* `"ortho_scale"`: Camera ortho-scale.

Gizmo will also read whatever the user asks for using VisScript.

## PROPS EMITTED BY DATAEXT

Firstly, everything read by Gizmo. But also:

* `"mode"`: As supplied by user
* `"female"`: 1 if female else 0
* `"male"`: 1 if male else 0
* `"age"`: Age number
* `"genus"`: Genus, as string
* `"gs_num"`: Genus/Sex number (0-7, as in sprite files)
* `"breed"`: Breed, as string
* `"breed_num"`: Breed, as 0-25 (for the letters A-Z, as in PoseEditor)
* `"part_ascii"`: Part ID, as the ASCII number, i.e. head (`a`) would be 97

