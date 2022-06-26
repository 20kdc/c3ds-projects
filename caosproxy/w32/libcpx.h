/*
 * caosprox - CPX server reference implementation
 * Written starting in 2022 by 20kdc
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

#pragma once

#include <winsock2.h>
#include <windows.h>

// Schedule:
// Breaded, Squeezed, Drunk, Underwater, Airlocked, Exported, Spliced
// ^
#define LIBCPX_VERSION_NAME "Breaded"

#define LIBCPX_VERSION LIBCPX_VERSION_NAME " [" __TIME__ " " __DATE__ "]"

// This also matches the shared memory layout (on purpose).
// This is also tied into protocol stuff.
// So if it isn't exactly 24 bytes, you screwed up!
typedef struct {
	char magic[4]; // cpx@
	DWORD pid; // by unspoken convention, 0 if not supported
	int resultCode, sizeBytes, maxSizeBytes, padding;
	char data[]; // Note that this can be thought of as being of length 0.
} libcpx_shmHeader_t;

// socket utilities

inline static int libcpx_sgetc(SOCKET s) {
	char chr;
	if (recv(s, &chr, 1, 0) == 1)
		return chr & 0xFF;
	return -1;
}

inline static int libcpx_sgeta(SOCKET s, void * target, int len) {
	char * targetC = target;
	int i = 0;
	while (i < len) {
		int res = recv(s, targetC, len - i, 0);
		if (res <= 0)
			return i;
		i += res;
		targetC += res;
	}
	return len;
}

inline static void libcpx_sputc(SOCKET s, char chr) {
	send(s, &chr, 1, 0);
}

inline static void libcpx_sputa(SOCKET s, const void * target, int len) {
	while (len > 0) {
		int res = send(s, target, len, 0);
		if (res <= 0)
			return;
		target += res;
		len -= res;
	}
}

inline static void libcpx_initWinsock() {
	WSADATA dontcare = {};
	WSAStartup(MAKEWORD(2, 2), &dontcare);
}

