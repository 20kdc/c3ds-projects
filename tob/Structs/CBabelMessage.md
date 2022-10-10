# CBabelMessage
Typically allocated on-stack, this class acts as a wrapper for an actual message.
The message can then be packed into a wrapped form with metadata.
Said packed form is described at [Packed Babel Message](../Formats/Packed_Babel_Message.md).

Structure
---------


* +0: VTable pointer
	* cbabelmessage_vtbl_t * vtbl
* +4: Message data
	* void * messageData
* +8: Message data length
	* size_t messageDataLen
* +12: major type (see BABEL_MESSAGE)
	* int majorType
* +16: Something ELSE
	* void * somethingElse
* +20: Something ELSE len
	* size_t somethingElseLen
* +24: Flag (as passed by [CBabelClient](./CBabelClient.md)::SendBinaryMessage)
* +28: something (8 bytes)
	* [B_UIN](./B_UIN.md) b_uin


VTable
------

``cbabelmessage_vtbl_t``


* +0: CBabelMessage::SetMessage([std:string](./std_string.md), [B_UIN](./B_UIN.md) *)
	* void * setMessageStdString
* +4: CBabelMessage::SetMessage(char *, [B_UIN](./B_UIN.md) *)
	* void * setMessageCString
* +8: CBabelMessage::SetMessage(void *, int, [B_UIN](./B_UIN.md), bool)
	* void * setMessageComplex
* +12: CBabelMessage::scalar_deleting_destructor
	* void * scalarDeletingDestructor


