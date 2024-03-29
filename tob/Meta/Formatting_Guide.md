# Formatting Guide

https://github.com/marktext/marktext/ is used for editing. Used to use Zim but this makes it harder to read online.

VTables are called vtbl always, and have their own struct definitions.

~~Use codeblocks for lists of fields~~, and use the following style:

* +4: Amounts of space octopi fought today.
  * int spaceOctopiFought

Ideally use links when other pages are involved.

For C_TID_BASE subclasses
-------------------------

* Name is a guess
* Transactional

Packet description

36 bytes

* Type: 0x14
* A/B: Server UID/HID
* C/D: ?
* Ticket number: [0 or Allocated or Expected or Ignored, or for Both-style packets, 0/Ignored is possible]
* Further data: 0/Ignored
* E: ?
* +32: This is something
  * int something

* Sent from [CBabelVirtualSocket](../Structs/CBabelVirtualSocket.md)::Connect(C_TID_VIRTUAL_CONNECT *)
* CTOS Example: dsprotocol/bleh/octogontransmit
* STOC Example: don.dump packet number 123

### Response

32 bytes

* Type: 0x14
* A/B: Server UID/HID
* C/D: ?
* Ticket number: Expected
* Further data: [Discarded/other meaning - remember that the client *always* reads further data for responses.]
* E: ?
