/*
 * caosprox - CPX server reference implementation
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

#pragma once

#include <winsock2.h>
#include <windows.h>

// Schedule:
// Breaded, Squeezed, Drunk, Underwater, Airlocked, Exported, Spliced
//                           ^
#define LIBCPX_VERSION_NAME "Underwater"

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

typedef struct libcpx_channel libcpx_channel_t;

typedef int (*libcpx_channelRecv_t)(libcpx_channel_t * self, char * buf, int len);
typedef int (*libcpx_channelSend_t)(libcpx_channel_t * self, const char * buf, int len);
typedef void (*libcpx_channelClose_t)(libcpx_channel_t * self);

// A channel wraps an underlying thing to communicate with.
// This was added because it appears Windows Defender ruins loopback TCP
//  communications.
struct libcpx_channel {
	libcpx_channelRecv_t recv;
	libcpx_channelSend_t send;
	// If you want to just release without closing, use free()
	libcpx_channelClose_t close;
};

// Can return NULL if malloc returns NULL (the socket is not closed in this case)
libcpx_channel_t * libcpx_channelFromSocket(SOCKET skt);
// Same.
libcpx_channel_t * libcpx_channelFromW32H(HANDLE skt);

inline static void libcpx_initWinsock() {
	WSADATA dontcare = {};
	WSAStartup(MAKEWORD(2, 2), &dontcare);
}

int libcpx_cGetA(libcpx_channel_t * s, void * target, int len);
void libcpx_cPutA(libcpx_channel_t * s, const void * target, int len);

