/*
 * creature-monitor - watching norns from afar?
 * Written starting in 2022 by 20kdc
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

#include "cpx.h"

CPXRequestResult::CPXRequestResult(const char * err) : resultCode(1), content(err, strlen(err) + 1) {
}

CPXRequestResult::CPXRequestResult(int resultCode, int length) : resultCode(resultCode), content(NULL, (size_t) length) {
}

bool CPXRequestResult::verifyMagic(CMSlice & cleanSlice) {
	if (resultCode)
		return false;
	if (content.length < 10)
		return false;
	if (content.first(10) != "CMMagicHD\n")
		return false;
	// the zero terminator is checked for here
	if (content.last(10) != CMSlice("CMMagicFT", 10))
		return false;
	cleanSlice = content.slice(10, content.length - 20);
	return true;
}

typedef struct {
	char magic[4]; // cpx@
	int pid; // by unspoken convention, 0 if not supported
	int resultCode, sizeBytes, maxSizeBytes, padding;
} libcpx_shmHeader_t;

static int readAllLoop(TCPsocket socket, char * ptr, int bytesRemaining) {
	while (bytesRemaining) {
		int res = SDLNet_TCP_Recv(socket, ptr, bytesRemaining);
		if (res <= 0)
			return 1;
		bytesRemaining -= res;
		ptr += res;
	}
	return 0;
}

CPXRequestResult * cpxMakeRawRequest(const char * request) {
	IPaddress ipa = {SDL_SwapBE32(INADDR_LOOPBACK), SDL_SwapBE16(19960)};
	TCPsocket socket = SDLNet_TCP_Open(&ipa);
	if (socket) {
		int reqSize = strlen(request) + 1;
		int reqSizeLE = SDL_SwapLE32(reqSize);

		libcpx_shmHeader_t shmHeader[2];

		// send the request
		SDLNet_TCP_Send(socket, &reqSizeLE, 4);
		SDLNet_TCP_Send(socket, request, reqSize);

		// read back headers
		if (readAllLoop(socket, (char *) shmHeader, sizeof(shmHeader))) {
			SDLNet_TCP_Close(socket);
			return new CPXRequestResult("header read termination");
		}

		CPXRequestResult * finale = new CPXRequestResult(shmHeader[1].resultCode, shmHeader[1].sizeBytes);
		if (readAllLoop(socket, finale->content.data, finale->content.length)) {
			delete finale;
			SDLNet_TCP_Close(socket);
			return new CPXRequestResult("data read termination");
		}
		SDLNet_TCP_Close(socket);

		return finale;
	} else {
		return new CPXRequestResult("failed to open socket");
	}
}

