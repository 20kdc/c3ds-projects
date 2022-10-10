# CBabelTransactionTicket
16 bytes


* +0: Ticket number
	* int ticketNumber
* +4: Flags (used for signalling completion)
	* byte flags
* +8: Inherent response size
	* int inherentResponseSize
* +12: Received data buffer
	* void * receivedBuffer


