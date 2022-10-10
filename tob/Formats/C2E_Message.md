# C2E Message
12 bytes - meant to act as an extensible type indicator, but seemingly never got expanded.


* +0: Always 12 (the length of this format). Note this does NOT include data.
	* int length
* +4: Type
	* int type
* +8: 0
	* int zero


Type 0: PRAY File
-----------------

Literally just a PRAY file.

Note though that [NetManager](../Structs/NetManager.md) will spool these with [InboxOutbox.](../Concepts/InboxOutbox.md)

Type 1: NET: WRIT
-----------------


* int channelLength
* char channel[channelLength]
* int messageId
* [PackedCaosVar](./PackedCaosVar.md) param1
* [PackedCaosVar](./PackedCaosVar.md) param2


