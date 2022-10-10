# Layers
Before going into this, it's worth noting that it seems different formats are used for the sending and receiving paths, though they go through the same classes.

This layers diagram has to be outwards-in because there's different paths and stuff.


* Initial connection
	* [CBabelClient](../Structs/CBabelClient.md)::Connect generates the C_TID_HANDSHAKE packet.
		* [CBabelClient](../Structs/CBabelClient.md)::Connectx sends it and receives the C_TID_HANDSHAKE_RESPONSE packet.
			* [CBabelAsyncCore](../Structs/CBabelAsyncCore.md)::Connect
				* WinSock



* CAOS commands, polling, etc - the NetManager layer
	* Varies in execution, but the *theoretical ideal* is:
		* Custom packets 'should' go through [CBabelClient](../Structs/CBabelClient.md)::SendMessageA
			* But some code likes to just write via the socket directly...
		* Custom transaction packets need to deal with [CBabelClient](../Structs/CBabelClient.md)::AddTransactionTicket/etc.
		* Player-to-player messaging (NET: WRIT/EXPO/MAKE) goes through the [C2E Message](../Formats/C2E_Message.md) format
			* NET: WRIT in particular goes through [NetManager](../Structs/NetManager.md)::PostDirectMessage
				* [CBabelClient](../Structs/CBabelClient.md)::ConnectToPeer
					* [CBabelClient](../Structs/CBabelClient.md)::GetConnectionDetail
						* [CBabelSocket](../Structs/CBabelSocket.md)::Write (to server)
					* TODO: All the spooky stuff - it seems like CBabelConnection's vtbl needs to be checked out
				* [NetDirectLink](../Structs/NetDirectLink.md)::SendData
					* *Can go via virtual circuits or via a direct link. Realistically, virtual circuits.*
						* [CBabelVirtualSocket](../Structs/CBabelVirtualSocket.md)::Write
							* [CBabelSocket](../Structs/CBabelSocket.md)::Write (to server)
			* Other messages go through [CBabelClient](../Structs/CBabelClient.md)::SendBinaryMessage



* Receipt of packets from the server
	* [CBabelDConnection](../Structs/CBabelDConnection.md)::Handle
		* [CBabelDConnection](../Structs/CBabelDConnection.md)::TicketDispatch
			* (presumably) completed ticket is checked by it's sender
		* [CBabelDConnection](../Structs/CBabelDConnection.md)::MessageDispatch
			* ClientCommand, message queue
				* NetManager message queue stuff
		* [CBabelDConnection](../Structs/CBabelDConnection.md)::UserOnline/UserOffline
			* ClientCommand, etc.
		* [CBabelDConnection](../Structs/CBabelDConnection.md)::ClientClientCommand
		* [CBabelDConnection](../Structs/CBabelDConnection.md)::OnlineChange
		* [CBabelDConnection](../Structs/CBabelDConnection.md)::VirtualCircuitConnect
		* [CBabelDConnection](../Structs/CBabelDConnection.md)::VirtualCircuit
		* [CBabelDConnection](../Structs/CBabelDConnection.md)::VirtualCircuitClose



* Creature history sending
	* NetworkImplementation::HistoryFeed
		* HistoryTransferOut::GetAddress does the actual packing *and knowing this will save you much pain*
		* HistoryQueuedMessage::Send
			* DSNetManager::DSFeedHistory <- FANCY 0x21 EXTENSION PACKET
				* *regular transaction infrastructure*


