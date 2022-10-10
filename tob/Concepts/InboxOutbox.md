# InboxOutbox
The Outbox is a disk buffer for files being sent over the internet.

Filenames in it represent information about the file.

The filename starts with the Unix time in seconds, followed by "T-", followed by a stringified [B_UIN](../Structs/B_UIN.md).

An example dummy filename: ``0T-1+1.warp``

The file in this form is expected to be a PRAY file.
The B_UIN part of the filename is extracted based on the position of the last - and the last . to get the destination.

It is then:

* Prepended with a [C2E Message](../Formats/C2E_Message.md) header at [NetManager](../Structs/NetManager.md)::SendOrdinaryMessages
* Passed to [CBabelClient](../Structs/CBabelClient.md)::SendBinaryMessage
* Which wraps it *again* as a [Packed Babel Message](../Formats/Packed_Babel_Message.md)
* And then sends it in a C_TID_MESSAGE package (see [:Packets:CTOS](../Packets/CTOS.md))
* ...Which is finally what is sent to the server.


*a series of pipes later...*


* The server (presumably) forwards this to the intended recipient, with possibly some fiddling.
* This hits [CBabelDConnection](../Structs/CBabelDConnection.md)::MessageDispatch, which attaches it to a message queue
* [NetManager](../Structs/NetManager.md)::SpoolMessages at some point notices and retrieves the message, putting it into it's own spool
* The file goes into the user's Inbox at [NetManager](../Structs/NetManager.md)::SpoolBinaryFile
* The "immigrant checker" CAOS performs a PRAY REFR, refreshing the engine's view of PRAY files on the system
* It then notices the added inbox PRAY file, and from there it's almost a normal creature import, nothing to see here


