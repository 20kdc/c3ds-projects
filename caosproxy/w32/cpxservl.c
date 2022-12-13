/*
 * caosprox - CPX server reference implementation
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

// Looper module - contains the server main loop
#include <stdlib.h>
#include <stdio.h>

#include "cpxservc.h"

static SOCKET serverSocket;

// Initialize
int cpxservl_serverInit(int host, int port) {
	// deal with WS nonsense
	libcpx_initWinsock();
	// actual stuff
	serverSocket = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
	if (serverSocket == INVALID_SOCKET) {
		puts("caosprox could not make socket");
		return 1;
	}
	// binding (here's the scary part)
	struct sockaddr_in bindTarget = {
		.sin_family = AF_INET,
		.sin_addr = {
			.s_addr = host
		},
		.sin_port = htons(port),
	};
	if (bind(serverSocket, (struct sockaddr *) &bindTarget, sizeof(bindTarget))) {
		puts("caosprox could not bind to socket");
		return 1;
	}
	// doing great
	if (listen(serverSocket, SOMAXCONN)) {
		puts("caosprox failed to listen on socket");
		return 1;
	}
	return 0;
}

// Main loop
void cpxservl_serverLoop() {
	while (1) {
		// get a client
		SOCKET client = accept(serverSocket, NULL, NULL);
		if (client == INVALID_SOCKET)
			continue;
		libcpx_channel_t * channel = libcpx_channelFromSocket(client);
		if (channel) {
			cpxservi_handleClient(channel);
			channel->close(channel);
		} else {
			// :(
			closesocket(client);
		}
	}
}

