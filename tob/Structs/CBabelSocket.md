# CBabelSocket
Really really abstract.

Structure
---------

4 bytes (it's very abstract)


* +0: VTable pointer
	* cbabelsocket_vtable_t vtbl


VTable
------

``cbabelsocket_vtbl_t``


* +0: scalar destructor
	* scalarDestructor
* +4: Read
	* read
* +8: Write
	* undefined4 __thiscall cbabelsocket_write_t(CBabelSocket *, undefined4, undefined4)
* +12: BytesReady
	* bytesReadyA
* +16: Close
	* undefined4 __thiscall cbabelsocket_close_t(CBabelSocket *)
* +20: Accept
	* accept
* +24: BytesReady
	* bytesReadyB



