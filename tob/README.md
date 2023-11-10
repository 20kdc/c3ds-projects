# T.O.B

Reverse-engineering information on the NetBabel protocol.

## Thanks

Thanks to Don & WizardNorn for the "don.dump" file that became the cornerstone of checking the theory against actual packets.
If not for these early efforts there might not have been anything to test with or examine.

And thanks to everyone who came before.

## A quick foreword

This documentation has been converted from Zim to Markdown, and beyond.

It's my working notes stapled together and published so that the NetBabel protocol details might not be lost to time as they once were.

As such, it's messy and it's ugly.

Still, I'm hoping that binding it in a coherent structure and sticking a foreword on it might help with... bearings.

## What Is NetBabel?

NetBabel is the protocol used by Docking Station for the Warp, the online component of the game.

NetBabel is essentially three protocols.

* The user/server protocol is the actual NetBabel protocol as used practically.
* The user/user protocol *should never be used. It was an attempt to save bandwidth. It's almost certainly a security hazard.*
* The server/server protocol is essentially completely unknown. It however is useful to keep in mind it exists. We can observe its effects on the user/server protocol, and in turn this explains oddities of the user/server protocol.

NetBabel is designed around the idea of there being a network of connected game servers. This aggregation lowers the load on any individual server.

The rest of this document will solely talk of the user/server protocol.

NetBabel has very little functionality:

* Login
* Submitting creature history
* Random user get
* User statuses/presence (online or offline)
* "Who's Wanted Register" (presence subscription)
* Sending and receiving PRAY files, with storage

All functions more advanced than these are implemented "in user-space" via CAOS.
See the High-Level section of the book for more information on that.

## Basic Operation

1. Client connects to server via TCP.
2. Client sends CTOS handshake packet.
3. Server sends STOC handshake response packet.
4. Client/server send packets indefinitely.

## The Purpose Of Each Section

* Concepts: Concepts you have to learn in order to understand what other parts of NetBabel do and why they do it.
* Formats: These are formats inside NetBabel, like file formats or blob formats. PRAY would be in here, but that's already documented well enough.
* High-Level: This covers "out-of-protocol" details - catalogue and CAOS changes could change these parts.
* Structs: Little parts of things.
* Packets: The main meat of NetBabel as a protocol.
* Internal Structs: This is where the reverse engineering information is kept, should it ever need to be recovered.
