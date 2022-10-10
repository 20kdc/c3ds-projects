# ClientMessages
These go through [CBabelClient](../Structs/CBabelClient.md)::ClientMessage through to [NetManager](../Structs/NetManager.md)::BabelCallback.
They consist of 3 integers, the first seemingly being type.


* 2/3,0,X: Fired from [CBabelDConnection](../Structs/CBabelDConnection.md)::UserOnline/UserOffline
	* X: Pointer to a [CBabelShortUserDataObject](../Structs/CBabelShortUserDataObject.md)
	* [NetManager](../Structs/NetManager.md) picks these up and updates accordingly.
* 5,0,X: Fired from [CBabelDConnection](../Structs/CBabelDConnection.md)::MessageDispatch
	* X: Amount of packets in [CBabelClient](../Structs/CBabelClient.md) message queue
* 7,X,Y: Fired from [CBabelDConnection](../Structs/CBabelDConnection.md)::ClientClientCommand
	* X: Sub-command number.
	* Y: Pointer to a structure, 12 bytes:
		* int targetUID
		* int targetHID
		* int subCommand (equal to X)
	* [NetManager](../Structs/NetManager.md) memory-leaks said structure.
* 9,0,0: Fired from [CBabelClient](../Structs/CBabelClient.md)::ConnectionLost
* 12,X,Y: Fired from [CBabelRemoteSwitch](../Structs/CBabelRemoteSwitch.md)::Disconnected
	* X: ?
	* Y: ?
* 13,0,0: Fired from [CBabelClient](../Structs/CBabelClient.md)::OnlineChange


