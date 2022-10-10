# CBabelClient
hopelessly incomplete description, held together by computer assistance and hope

Structure
---------


* +16: switch box
	* [CBabelSwitchBox](./CBabelSwitchBox.md) switchBox
* +32: User BUIN (as returned by GetUser)
	* [B_UIN](./B_UIN.md) userBUIN
* +40: async core
	* [CBabelAsyncCore](./CBabelAsyncCore.md) asyncCore
* +100:
	* [CBabelDConnection](./CBabelDConnection.md) connection
* +144: server info?
	* [CBabelServerInfo](./CBabelServerInfo.md) * serverInfo
* +156: [std_vector](./std_vector.md)<[B_UIN](./B_UIN.md)> Online user IDs
	* [B_UIN](./B_UIN.md) * usersOnline
	* [B_UIN](./B_UIN.md) * usersOnlineEnd
* +172: Server HID (?)
	* int serverHID
* +176: Server UID (?)
	* int serverUID
* +180: Next ticket number (as seen in GetConnectionDetail)
* +184: Client peer link
	* [CBabelClientPeerLink](./CBabelClientPeerLink.md) * peerLink
* +192: [std_vector](./std_vector.md) of dwords
	* ? unks
	* ? unksEnd
* +204: client peer handler callback
	* void * peerHandlerCallback
	* [NetManager](./NetManager.md) sets this to NetManager::PeerHandlerStatic(C_BABELCLIENT_BASE *, CBabelSocket *, CBabelClient *)
* +212: critical section
	* _RTL_CRITICAL_SECTION criticalSection
* +240: [std_vector](./std_vector.md)<[CBabelTransactionTicket](./CBabelTransactionTicket.md) *> Transaction tickets
	* [CBabelTransactionTicket](./CBabelTransactionTicket.md) ** transactionTickets
	* [CBabelTransactionTicket](./CBabelTransactionTicket.md) ** transactionTicketsEnd
* +252: Error code
	* int errorCode
* +260: ???
	* [CBabelAsyncCoreChild](./CBabelAsyncCoreChild.md) ** ???
* +264: client callbacks (4 of them)
	* FPTR * rawPacketCallbacks
* +268: client message callback
	* void * clientMessageCallback
	* [NetManager](./NetManager.md) sets this to NetManager::BabelCallbackStatic(CBabelClient *,uint,int,int)
* +288: port (as returned by GetPort)
	* int port
* +312: user data???
	* [CBabelShortUserDataObject](./CBabelShortUserDataObject.md)
* +376: "IChannel" socket (see CloseIChannel)
	* SOCKET iChannelSocket
* +384: [std_vector](./std_vector.md)<[CBabelMessage](./CBabelMessage.md) *> Message queue for MessageDispatch packets
	* [CBabelMessage](./CBabelMessage.md) ** messageQueue
	* [CBabelMessage](./CBabelMessage.md) ** messageQueueEnd
* +416: critical section 2
	* _RTL_CRITICAL_SECTION criticalSection2
* +440: async core 2
	* [CBabelAsyncCore](./CBabelAsyncCore.md) asyncCore2
* more as of yet unknown


