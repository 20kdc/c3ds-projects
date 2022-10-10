# Packets
Master list of packet numbers
-----------------------------


* 0x09: MessageDispatch (CTOS, STOC)
* 0x0A: Handshake Response (STOC)
* 0x0D: UserOnline (STOC)
* 0x0E: UserOffline (STOC)
* 0x0F: GetClientInfo (CTOS)
* 0x10: AddWWREntry (CTOS)
* 0x11: RemoveWWREntry (CTOS)
* 0x12: NotifyListeningPort (CTOS)
* 0x13: GetConnectionDetail (CTOS)
* 0x14: ClientCommand (CTOS, STOC)
* 0x1D: OnlineChange (STOC)
* 0x1E: VirtualCircuitConnect (CTOS, STOC)
* 0x1F: VirtualCircuit (STOC)
* 0x20: VirtualCircuitClose (STOC)
* 0x21 through 0x24 inclusive: STOC shunts around weirdly, may imply something odd going on in [CBabelDConnection](./Structs/CBabelDConnection.md)
	* 0x0221: DSFetchRandomUser (CTOS)
* 0x25: Handshake (CTOS)


*For call sites or pretty much any further information, please see either STOC or CTOS.*

