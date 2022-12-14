/*
 * caosprox - CPX server reference implementation
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

#include "libcpx.h"

// socket channels

typedef struct {
	libcpx_channel_t head;
	SOCKET socket;
} libcpx_channelSocket_t;

static int socketSend(libcpx_channel_t * self, const char * buf, int len) {
	return send(((libcpx_channelSocket_t *) self)->socket, buf, len, 0);
}

static int socketRecv(libcpx_channel_t * self, char * buf, int len) {
	return recv(((libcpx_channelSocket_t *) self)->socket, buf, len, 0);
}

static void socketClose(libcpx_channel_t * self) {
	closesocket(((libcpx_channelSocket_t *) self)->socket);
	free(self);
}

libcpx_channel_t * libcpx_channelFromSocket(SOCKET skt) {
	libcpx_channelSocket_t * st = malloc(sizeof(libcpx_channelSocket_t));
	if (!st)
		return NULL;
	st->head.recv = socketRecv;
	st->head.send = socketSend;
	st->head.close = socketClose;
	st->socket = skt;
	return (libcpx_channel_t *) st;
}

// Named pipe nonsense channels

typedef struct {
	libcpx_channel_t head;
	HANDLE handle;
} libcpx_channelW32H_t;

static int handleSend(libcpx_channel_t * self, const char * buf, int len) {
	HANDLE h = ((libcpx_channelW32H_t *) self)->handle;
	DWORD res = 0;
	if (WriteFile(h, buf, (DWORD) len, &res, NULL)) {
		// Succeeded (supposedly)
		return (int) res;
	} else {
		// Failed
		return -1;
	}
}

static int handleRecv(libcpx_channel_t * self, char * buf, int len) {
	HANDLE h = ((libcpx_channelW32H_t *) self)->handle;
	DWORD res = 0;
	if (ReadFile(h, buf, (DWORD) len, &res, NULL)) {
		// Succeeded (supposedly)
		return (int) res;
	} else {
		// Failed
		return -1;
	}
}

static void handleClose(libcpx_channel_t * self) {
	HANDLE h = ((libcpx_channelW32H_t *) self)->handle;
	CloseHandle(h);
	free(self);
}

libcpx_channel_t * libcpx_channelFromW32H(HANDLE skt) {
	libcpx_channelW32H_t * st = malloc(sizeof(libcpx_channelW32H_t));
	if (!st)
		return NULL;
	st->head.recv = handleRecv;
	st->head.send = handleSend;
	st->head.close = handleClose;
	st->handle = skt;
	return (libcpx_channel_t *) st;
}

// Utilities

int libcpx_cGetA(libcpx_channel_t * s, void * target, int len) {
	char * targetC = target;
	int i = 0;
	while (i < len) {
		int res = s->recv(s, targetC, len - i);
		if (res <= 0)
			return i;
		i += res;
		targetC += res;
	}
	return len;
}

void libcpx_cPutA(libcpx_channel_t * s, const void * target, int len) {
	while (len > 0) {
		int res = s->send(s, target, len);
		if (res < 0)
			return;
		target += res;
		len -= res;
	}
}


