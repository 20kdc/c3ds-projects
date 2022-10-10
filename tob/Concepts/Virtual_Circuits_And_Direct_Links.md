# Virtual Circuits And Direct Links
So part of the design goals for Babel were presumably that computers should be able to make P2P connections by themselves.
*But* either the reality of NAT'd/firewalled connections set in, or someone realized there was zero verification to stop anyone pretending to be anyone.
Notably, the functionality to tell the server about the opened port is missing.

As such, Babel has the concept of VirtualSockets, i.e. [CBabelVirtualSocket](../Structs/CBabelVirtualSocket.md).
This is simply a socket layer over the main Babel connection.
Wrapping this is [NetDirectLink,](../Structs/NetDirectLink.md) which can either connect virtually or directly.

Simply put, NetDirectLink is a socket that *may* be connected virtually or directly, and a caller does not have to care which.
There are other components to this but none of them matter.

***It is worth noting that as far as I'm aware, the only functionality that initiates a Direct Link connection is NET: WRIT.***
***NET: WRIT also isn't actually used in the game, and has a bug where it can and will freeze the game if anything goes wrong whatsoever.***
***Plus, NET: WRIT can be used to trivially forge messages. There are no checks.***
***With this in mind, a server reimplementation should pretend to accept the virtual circuit connection on the receiver's behalf, using the initiator's VSN as the recipient's VSN.***
***It is up to the server reimplementation to decide how it wants to handle the contents - discarding them entirely or attempting to parse them are perfectly valid choices.***

Virtual Circuits
----------------

The clients will be referred to as *Initiator*  and *Recipient* as they do not follow a strict client/server relationship, and this gets confusing anyway.

### Creation


* *Initiator* sends C_TID_VIRTUAL_CONNECT packet
	* [CBabelVirtualSocket](../Structs/CBabelVirtualSocket.md)::Connect(CBabelVirtualSocket *,B_UIN *)
* Server transports to other side
* *Recipient* responds with a C_TID_CLIENT_COMMAND
	* [CBabelVirtualSocket](../Structs/CBabelVirtualSocket.md)::Connect(CBabelVirtualSocket *,C_TID_VIRTUAL_CONNECT *)
	* Details:
		* E: Upper half is the original VSN from the connect packet, lower half is the local VSN.
		* subCommand: 0xE
* Server transports to other side
* The connection is officially created and both clients know both VSNs.


### Data Transfer


* One of the clients send a C_TID_VIRTUAL_CIRCUIT packet with the data, see packet description for details
* Server transports to other side
* Packet's receiver accepts the data and all is well


### Closing

*About that...*
The server seems to be able to terminate a connection, but the client doesn't seem to. Very odd.

NetDirectLink
-------------

*TODO: Something about the NetDirectLink header here. Need to work out CBabelConnection VTBL before continuing that though.*
*This may not even matter, as the server doesn't really have cause to be snooping around anyway.*
*It may just be an idea to block the virtual circuit system entirely, it's not like the game uses it.*

