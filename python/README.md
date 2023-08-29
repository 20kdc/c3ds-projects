# c3ds-projects Python scripts/libraries

This is a set of Python 3 scripts for various poking.
Note that those scripts which connect to a CPX server use `CPX_HOST` and `CPX_PORT`, or default to localhost and 19960.

## `caosterm.py`

CPX CAOS terminal.

## `maprooms.py`

`maprooms.py [a quoted list of space separated metaroom IDs] > some.svg`

Maps the rooms in the running game.

## `maptypes.py`

Provides a list of classifiers which have scripts.

## `bdmptest.py`

For testing of the handling of binary data by CPX servers, runs "brn: dmpl 0" for the selected creature

## `cpxciesv.py` (CAOSProxy Server for Linux Docking Station)

This is by no means complete but it's the sort of thing where it should be very easy to deal with the issues.

Known hazards:

1. Errors are not detected
2. Variant command leaders (different forms of `execute\n`, etc.) are not properly detected
3. The magic `\nrscr` string appearing in the script to send is NOT checked for

Binary data transmission at least works properly though.

## `libkc3ds`

This is a library for doing all sorts of fun things:

* CPX client/server utilities
* C16/S16 import/export

*Important: Loading code in this library has to work in Blender because of The Scary Unnamed Project Of Indeterminate Duration.*

*Any library imports that wouldn't work in a stock Blender 3.x install need to avoid being loaded by that project.*

