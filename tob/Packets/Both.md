# Both
C_TID_CLIENT_COMMAND
--------------------


* Name is a guess


Used to notify the client/another client of events, at least in theory.
In practice, this packet is basically a landmine.
It does moonlight as a "connection accepted" mechanism for the Virtual Circuits mechanism, which is always fun...

36 bytes


* Type: 0x14
* A/B: Server UID/HID (STOC ignores)
* C/D: Target UID/HID (STOC ignores, CTOS copies from STOC C_TID_VIRTUAL_CONNECT - i.e. sends to initiator)
	* STOC-wise, for "normal" subCommand values, this information is put into a newly made struct which is promptly memory-leaked on the [NetManager](../Structs/NetManager.md) side.
* Ticket number: 0/Ignored
* Further data: 0/Ignored
* E: Only matters for virtual circuit accept packets. Upper half is the original VSN from the C_TID_VIRTUAL_CONNECT packet, lower half is the local VSN. See [Virtual Circuits And Direct Links](../Concepts/Virtual_Circuits_And_Direct_Links.md). **It is worth noting that 0 is not considered a valid response VSN and will cause NET: WRIT to freeze as usual.**
* +32: 0xE is a special value here.
	* int subCommand
	* If this is 0xE, a client receiving this passes it into the virtual circuit system - see [Virtual Circuits And Direct Links](../Concepts/Virtual_Circuits_And_Direct_Links.md).
	* *Otherwise*, this is passed to [ClientMessages](../Concepts/ClientMessages.md).



* Sent from [CBabelVirtualSocket](../Structs/CBabelVirtualSocket.md)::Connect(C_TID_VIRTUAL_CONNECT *)
* Received by [CBabelDConnection](../Structs/CBabelDConnection.md)::ClientClientCommand
* *No known example*


C_TID_VIRTUAL_CONNECT
---------------------

Opens a virtual socket between two clients.
For further information, see [Virtual Circuits And Direct Links](../Concepts/Virtual_Circuits_And_Direct_Links.md).
*Worth noting: in the one situation in which the client will send this packet, a response must be given or the game freezes.*

**While it would seem pointless to implement ever receiving this packet, Natsue uses it as a ping mechanism, starting a virtual circuit from the server's UIN. Once the acknowledgement ClientCommand is received, Natsue sends back a C\_TID\_VIRTUAL\_CIRCUIT\_CLOSE packet to clean things up on the client's end.**

**Just to be clear: There is no reason to, as a client developer, implement virtual circuits in a fashion any deeper than acknowledging incoming circuits with client command responses. These responses must use the correct initiator VSN, but you don't have to manage your own VSNs.**

44 bytes


* Type: 0x1E
* A/B: Server UID/HID
	* The client sending this (CTOS-wise) sets this - it's ignored by the receiving client.
* C/D: Initiator UID/HID
	* The client sending this (CTOS-wise) sets this as if from [CBabelClient](../Structs/CBabelClient.md)::GetUser.
	* Receiving client (STOC-wise) uses this as expected.
* Ticket number: 0/Ignored
* Further data: 0/Ignored
* E: Initiator's Virtual Socket Number
* +32/+36: Target UID/HID (STOC ignores)
* +40: 2 (STOC ignores)



* Sent from [CBabelVirtualSocket](../Structs/CBabelVirtualSocket.md)::Connect(B_UIN *)
* Received by [CBabelVirtualSocket](../Structs/CBabelVirtualSocket.md)::Connect(CBabelVirtualSocket *, C_TID_VIRTUAL_CONNECT *)
* *No known example*


C_TID_VIRTUAL_CIRCUIT
---------------------


* Name is a guess


Transmission of data through a virtual circuit.
For further information, see [Virtual Circuits And Direct Links](../Concepts/Virtual_Circuits_And_Direct_Links.md).

44 bytes + data


* Type: 0x1F
* A/B: Server UID/HID
	* The client sending this (CTOS-wise) sets this - it's ignored by the receiving client.
* C/D: Sender UID/HID
	* The client sending this (CTOS-wise) sets this as if from [CBabelClient](../Structs/CBabelClient.md)::GetUser - it's ignored by the receiving client.
* Ticket number: 0
* Further data: Exactly as much data as is being written.
* E: High short is target VSN, low short is sender VSN
	* STOC checks these - target must match the sender VSN of a [CBabelVirtualSocket,](../Structs/CBabelVirtualSocket.md) and sender should match the target VSN of that socket.
* +32/+36: Target UID/HID  (STOC ignores)
* +40: 2 (STOC ignores)



* Sent from [CBabelVirtualSocket](../Structs/CBabelVirtualSocket.md)::Write
* Received by [CBabelDConnection](../Structs/CBabelDConnection.md)::VirtualCircuit
* *No known example*


