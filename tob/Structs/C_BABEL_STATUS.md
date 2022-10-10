# C BABEL STATUS
16 bytes

Important to [CBabelClient](./CBabelClient.md)::GetStatus and the associated C_TID_GET_STATUS.

The following are named by NET: STAT's meanings. The order swapping occurs in [NetManager](./NetManager.md)::GetStatus.


* +0: time online
	* int timeOnline
* +4: users online
	* int usersOnline
* +8: bytes sent
	* int bytesSent
* +12: bytes received
	* int bytesReceived


