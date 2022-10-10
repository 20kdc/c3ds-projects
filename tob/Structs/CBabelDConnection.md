# CBabelDConnection
Subclass of [CBabelSocket](./CBabelSocket.md) and [CBabelAsyncCore](./CBabelAsyncCore.md)

Appears to handle the basic client receipt framing - see CBabelDConnection::Handle.

Structure
---------

Presently assumed to be 76 bytes.


* +0: async core base
	* [CBabelAsyncCore](./CBabelAsyncCore.md) baseAsyncCore
* +60: socket base
	* [CBabelSocket](./CBabelSocket.md) baseSocket
* +72: parent client
	* [CBabelClient](./CBabelClient.md) * client


