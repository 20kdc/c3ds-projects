# CBabelVirtualSocket
0x40 bytes (CERTAIN)


* +0: VTable Pointer
* +16: Virtual socket number acquired with GetNewVirtualSocket
	* short virtualSocketNum
* +18: padding?
* +20: client pointer
	* [CBabelClient](./CBabelClient.md) * client
* +24: Remote VSN. 0 is invalid. Also used as an "am I connected?" flag.
	* int remoteVSN
* +40: _RTL_CRITICAL_SECTION (16 bytes)


