# Known PRAY Chunks

This is meant to represent a complete listing of all PRAY chunks that a vanilla Docking Station Build 195 instance will send or receive under any circumstances that do not involve external sources of CAOS (including but not limited to the CAOS console).

## "GLST"

Life history of a warped creature.

*Not a [Creature History Blob](../Formats/Creature_History_Blob.md). This is still CreaturesArchive data. But similar in intent.*

Uses the parent chunk's name followed by `.glist.creature` (i.e. `002-hill-(...)-6zzex.warp.glist.creature`).

## "CREA"

Main data of a warped creature.

Uses the parent chunk's name followed by `.creature` (i.e. `002-hill-(...)-6zzex.warp.creature`).

## "GENE"

Genetics of a warped creature.

Uses the parent chunk's name followed by `.genetics` (i.e. `002-hill-(...)-6zzex.warp.genetics`).

## "PHOT"

Photo of a warped creature.

Naming is based on the photo filename followed by a general suffix. Such a suffix is usually, say, `.warp.photo`.

The photo filename style is consistent and is laid out in `DS creature history.cos` and `DS life event factory - PHOTOGRAPHS THE DEAD.cos`: `MONIKER-EVENTINDEX`.

**These event indexes are not Solid Indexes.** Overlap is possible.

So, a real instance in full is: `001-dawn-6wa4r-az8x7-cnv4v-ulggk-3.DSEX.photo`.

## "warp"

Warped creature, suitable for importing with `PRAY IMPO`. Uses a resource name of the creature's moniker followed by `.warp`.

Some extra tags are applied via the "Pray Extra (...)" mechanism:

+ `"Pray Extra foe"`: Part of rejection.

+ `"Pray Extra reject"`: Part of rejection.

## "MESG"

Mail sent via the Message Centre.

+ `"Sender UserID"`: String, stringified [B_UIN](../Structs/B_UIN.md)
  + Client will blindly believe this, *except* during foe checking
+ `"Sender Nickname"`: String
  + Client will also blindly believe this
+ `"Date Sent"`: String
  + This is intended to be in a specific format `yyyymmddhhmmss` and will be blindly cut-up to format it.
+ `"Subject"`: String
+ `"Message"`: String

Messages can be 12 lines, assuming no wrapping occurs.

## "CHAT"

Various kinds of chat message are sent via this chunk type.

## "REQU"

Various kinds of chat request are sent via this chunk type.

## "INVI"

This is in the client's whitelist of PRAY chunks, but isn't actually supported, so if received it will stick around forever unless something else is attached. For obvious reasons, filter this chunk.
