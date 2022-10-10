# CBabelServerInfo
Useful notes:
``SetServer(host, name, port, id)``

Structure
---------

40 bytes


* +0: host
	* [std_string](./std_string.md) host (16 bytes)
* +16: friendly name
	* [std_string](./std_string.md) name (16 bytes)
* +32: port
	* int port



