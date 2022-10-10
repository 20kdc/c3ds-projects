# Packed Babel Message
Packed using [CBabelMessage](../Structs/CBabelMessage.md).

Binary "Packed" Format
----------------------

Firstly, the 24-byte header:


* +0: total size of message, including this field
	* size_t fullLen
* +4: Sender HID (Yes, this is in reverse order.)
	* short hid
* +6: 2 bytes of uninitialized padding
	* short padding
* +8: Sender UID
	* int uid
* +12: Message data length
	* size_t messageDataLen
* +16: Something ELSE length
	* size_t somethingElseLen
* +20: major type (see BABEL_MESSAGE)
	* int majorType


Secondly, the message data follows.
Thirdly, the "something else" follows.

Major Types
-----------

1: BABEL_MESSAGE_BINARY
-----------------------

This is what's output by [CBabelClient](../Structs/CBabelClient.md)::SendBinaryMessage.

