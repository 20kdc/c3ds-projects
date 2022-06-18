# CAOSProxy

CAOSProxy, or CPX, is a network-transparent protocol to connect to the C2E shared memory interface described at http://double.nz/creatures/developer/sharedmemory.htm .

It is intended for mixed environments where the game or applications may not be running natively.

For further information see `spec.txt`.

## `tools`

This is a set of Python 3 scripts for various poking.
Note that those scripts which connect to a CPX server use `CPX_HOST` and `CPX_PORT`, or default to localhost and 19960.

```
caosterm.py
CAOS terminal. Enough said.
```

```
maprooms.py [a quoted list of space separated metaroom IDs] > some.svg
Maps the rooms in the running game.
```

```
maptypes.py
Provides a list of classifiers which have scripts.
```

```
bdmptest.py
For testing of the handling of binary data by CPX servers, runs "brn: dmpl 0" for the selected creature
```

```
libcpx.py
this isn't a tool, it's a library
```

## `w32` (CAOSProxy Server W32)

This is a CAOSProxy (CPX) server to be used on Wine or on Windows.

```
caosprox [loud/quiet/invisible/help] [HOST] [PORT] [game name]
default mode is loud, HOST is 127.0.0.1 (set to 0.0.0.0 for remote access), default PORT is 19960, default game is autodetected
all prior args must be specified if you wish to use one
```

## `cpxciesv.py` (CAOSProxy Server for Linux Docking Station)

This is by no means complete but it's the sort of thing where it should be very easy to deal with the issues.

Known hazards:

1. Errors are not detected
2. Variant command leaders (different forms of `execute\n`, etc.) are not properly detected
3. The magic `\nrscr` string appearing in the script to send is NOT checked for

Binary data transmission at least works properly though.

