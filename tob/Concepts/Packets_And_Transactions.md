# Packets And Transactions

In the Babel protocol as seen from our client-server perspective, the basic framing of the protocol is this:

* All packets have a 32-byte header. The meanings of the fields in this header vary, but it is considered the same struct.
* For most packets, there is no algorithmic way to, independent of packet type, determine the size. This is even true if you have the header and know the packet types, as key details of packet length are sometimes after the 32-byte barrier. It is, however, a safe rule that for any given combination of packet header, direction (CTOS/STOC), and if the packet is a transaction response or not, there are a fixed amount of bytes to read to get the information required to get the length of the rest of the packet. 
* Some packets the client can send to the server start transactions, using a "ticket number". This "ticket number" is then used in the response packet. Ticket numbers are always non-zero.
* Transaction response packets always have a size equal to a constant known when the client sends the packet, plus a variable amount of further data indicated by a header field. The client will read further data even if not expected -- this is a feature of the framing.
* In some cases, the ticket number is the only difference between a transaction response and a different kind of packet.
* Packets that are client to server or server to client may have different formats, even given the same packet type.

