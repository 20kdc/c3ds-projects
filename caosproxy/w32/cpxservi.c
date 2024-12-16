/*
 * caosprox - CPX server reference implementation
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

// With extreme thanks to http://double.nz/creatures/developer/sharedmemory.htm

// Internals module - contains the actual SHM/CPX logic
#include <stdlib.h>
#include <stdio.h>
#include <winsock2.h>
#include <windows.h>

#include "cpxservc.h"

const char * cpxservi_gameID = NULL;
const char * cpxservi_gamePath = NULL;

#define GENERAL_STRBUF_LEN 2048
// above + 8 chars for _request
#define TMPBUF_LEN (GENERAL_STRBUF_LEN + 8)
// above - 24 bytes for SHM header (for snprintf shenanigans)
#define TMPBUF_RSPLEN (TMPBUF_LEN - sizeof(libcpx_shmHeader_t))
typedef struct {
	// Warning: This isn't the actual game ID 100% of the time, just allocated memory
	char gameIDBuf[GENERAL_STRBUF_LEN];
	// Same
	char gamePathBuf[GENERAL_STRBUF_LEN];
	char tmpBuf[TMPBUF_LEN];
	libcpx_channel_t * client;
} rGlobals_t;

static const char * findRegKey(HKEY hive, const char * base, const char * sub, const char * def, char * buffer) {
	HKEY key;
	if (!RegOpenKeyA(hive, base, &key)) {
		LONG bufferLen = 8191;
		DWORD throwaway; // I feel like there's a story to this being required.
		int result = RegQueryValueExA(key, sub, NULL, &throwaway, buffer, &bufferLen);
		RegCloseKey(key);
		if (!result) {
			// only try this if it succeeds - ERROR_MORE_DATA will cause it to run off the end of the buffer
			buffer[bufferLen] = 0;
			return buffer;
		}
		printf("caosprox registry warning, found \"%s\" but asking for \"%s\" got %x\n", base, sub, result);
	} else {
		printf("caosprox registry warning, failed to find \"%s\":\"%s\"\n", base, sub);
	}
	return def;
}

static const char * findGameID(rGlobals_t * g) {
	if (cpxservi_gameID)
		return cpxservi_gameID;
	return findRegKey(HKEY_CURRENT_USER, "Software\\CyberLife Technology\\Creatures Engine", "Default Game", "Docking Station", g->gameIDBuf);
}

static const char * findGamePath(rGlobals_t * g, const char * gameID) {
	if (cpxservi_gamePath)
		return cpxservi_gamePath;
	const char * base = "Software\\CyberLife Technology\\";
	int baseLen = strlen(base);
	char name[baseLen + strlen(gameID) + 1];
	strcpy(name, base);
	strcpy(name + baseLen, gameID);
	return findRegKey(HKEY_LOCAL_MACHINE, name, "Main Directory", "C:\\Program Files (x86)\\Docking Station\\", g->gamePathBuf);
}

static void transferAreaToClient(rGlobals_t * g, const libcpx_shmHeader_t * area, int hdrOnly) {
	libcpx_cPutA(g->client, area, hdrOnly ? 24 : (area->sizeBytes + 24));
}

static void sendStringResponse(rGlobals_t * g, const char * text) {
	libcpx_shmHeader_t * tmpErr = (libcpx_shmHeader_t *) g->tmpBuf;
	memcpy(tmpErr->magic, "c2e@", 4);
	snprintf(tmpErr->data, TMPBUF_RSPLEN, "%s", text);
	tmpErr->pid = 0;
	tmpErr->resultCode = 0;
	tmpErr->sizeBytes = strlen(tmpErr->data) + 1;
	tmpErr->maxSizeBytes = 8192;
	tmpErr->padding = 0;
	transferAreaToClient(g, tmpErr, 0);
}

#define EM_INIT 0
#define EM_SENT_H1 1
#define EM_RECEIVED_SIZE 2
#define EM_RECEIVED_BODY 3

static void internalError(rGlobals_t * g, const char * text, int errorMode, int reqSize) {
	// initialize header buffer
	libcpx_shmHeader_t * tmpErr = (libcpx_shmHeader_t *) g->tmpBuf;
	memcpy(tmpErr->magic, "c2e@", 4);
	snprintf(tmpErr->data, TMPBUF_RSPLEN, "caosprox: %s", text);
	tmpErr->pid = 0;
	tmpErr->resultCode = 1;
	tmpErr->sizeBytes = strlen(tmpErr->data) + 1;
	tmpErr->maxSizeBytes = 8192;
	tmpErr->padding = 0;
	// alright, work out what to do based on error mode
	if (errorMode == EM_INIT) {
		transferAreaToClient(g, tmpErr, 1);
		errorMode = EM_SENT_H1;
	}
	if (errorMode == EM_SENT_H1) {
		if (libcpx_cGetA(g->client, &reqSize, 4) != 4) {
			// no request size, so skip ahead
			errorMode = EM_RECEIVED_BODY;
		} else {
			errorMode = EM_RECEIVED_SIZE;
		}
	}
	if (errorMode == EM_RECEIVED_SIZE) {
		// receive body
		char req[1024];
		while (reqSize > sizeof(req)) {
			if (libcpx_cGetA(g->client, req, sizeof(req)) != sizeof(req))
				break;
			reqSize -= sizeof(req);
		}
		// if that didn't get interrupted, receive remainder
		if (reqSize > 0)
			if (reqSize <= 1024)
				libcpx_cGetA(g->client, req, reqSize);
		// done!
		errorMode = EM_RECEIVED_BODY;
	}
	transferAreaToClient(g, tmpErr, 0);
}

static void handleClientWithEverything(rGlobals_t * g, const char * gameID, libcpx_shmHeader_t * shm, HANDLE resultEvent, HANDLE requestEvent, HANDLE process) {
	// send SHM state to client
	transferAreaToClient(g, shm, 1);
	// now we want a size back
	int size;
	if (libcpx_cGetA(g->client, &size, 4) != 4) {
		// we failed to get request size, so skip ahead
		internalError(g, "failed to get request size", EM_RECEIVED_BODY, 0);
		return;
	}
	if (size < 0) {
		// deadlock bait, but what can you do?
		internalError(g, "request size under 0", EM_RECEIVED_BODY, 0);
		return;
	}
	if (size > shm->maxSizeBytes) {
		internalError(g, "request size exceeds maximum size", EM_RECEIVED_SIZE, size);
		return;
	}
	// can't hurt
	shm->sizeBytes = size;
	if (libcpx_cGetA(g->client, shm->data, size) != size) {
		internalError(g, "failed to get request body", EM_RECEIVED_BODY, 0);
		return;
	}
	// WAIT! This could be intended for CPX.
	if ((size >= 8) && !memcmp(shm->data, "cpx-fwd:", 8)) {
		// The client wants us to forward this along as if nothing happened.
		memmove(shm->data, shm->data + 8, size - 8);
		// fallthrough
	} else if ((size >= 8) && !memcmp(shm->data, "cpx-ver\n", 8)) {
		// The client wants us to give an identifier
		sendStringResponse(g, "CPX Server W32, version " LIBCPX_VERSION);
		return;
	} else if ((size >= 8) && !memcmp(shm->data, "cpx-gamepath\n", 13)) {
		// The client wants us to give the game path
		sendStringResponse(g, findGamePath(g, gameID));
		return;
	} else if ((size >= 4) && !memcmp(shm->data, "cpx-", 4)) {
		// It's intended for CPX but we don't recognize it.
		internalError(g, "Unrecognized CPX extension command.", EM_RECEIVED_BODY, 0);
		return;
	}
	// It's intended for the engine
	ResetEvent(resultEvent);
	PulseEvent(requestEvent);
	HANDLE waitHandles[2] = {process, resultEvent};
	DWORD result = WaitForMultipleObjects(2, waitHandles, FALSE, INFINITE);
	if (result == WAIT_OBJECT_0) {
		// This specific error means the process ended while the request was occuring.
		// This can be tested with the "bang" CAOS command or some other crashing operation.
		internalError(g, "game closed during request", EM_RECEIVED_BODY, 0);
	} else {
		// send the results back to the client
		transferAreaToClient(g, shm, 0);
	}
}

static void handleClientWithSHM(rGlobals_t * g, const char * gameID, libcpx_shmHeader_t * shm) {
	HANDLE process = OpenProcess(PROCESS_ALL_ACCESS, FALSE, shm->pid);
	if (!process) {
		internalError(g, "failed to open process handle (game dead?)", EM_INIT, 0);
		return;
	}
	sprintf(g->tmpBuf, "%s_result", gameID);
	HANDLE resultEvent = OpenEventA(EVENT_ALL_ACCESS, FALSE, g->tmpBuf);
	if (!resultEvent) {
		internalError(g, "failed to open result handle", EM_INIT, 0);
		CloseHandle(process);
		return;
	}
	sprintf(g->tmpBuf, "%s_request", gameID);
	HANDLE requestEvent = OpenEventA(EVENT_ALL_ACCESS, FALSE, g->tmpBuf);
	if (!requestEvent) {
		internalError(g, "failed to open request handle", EM_INIT, 0);
		CloseHandle(resultEvent);
		CloseHandle(process);
		return;
	}
	handleClientWithEverything(g, gameID, shm, resultEvent, requestEvent, process);
	CloseHandle(requestEvent);
	CloseHandle(resultEvent);
}

static void handleClientInsideMutex(rGlobals_t * g, const char * gameID) {
	sprintf(g->tmpBuf, "%s_mem", gameID);
	HANDLE fma = OpenFileMappingA(FILE_MAP_ALL_ACCESS, FALSE, g->tmpBuf);
	if (!fma) {
		internalError(g, "could not open memory handle (game not running/detection failed?)", EM_INIT, 0);
		return;
	}
	libcpx_shmHeader_t * shm = (libcpx_shmHeader_t *) MapViewOfFile(fma, FILE_MAP_ALL_ACCESS, 0, 0, 0);
	if (!shm) {
		internalError(g, "could not map view of shared memory", EM_INIT, 0);
		CloseHandle(fma);
		return;
	}
	handleClientWithSHM(g, gameID, shm);
	UnmapViewOfFile(shm);
	CloseHandle(fma);
}

void cpxservi_handleClient(libcpx_channel_t * client) {
	// notify UI thread for blinkenlights (moved here because of plan for multiple contact methods)
	cpxservg_activity();
	// continue...
	rGlobals_t g;
	g.client = client;
	const char * gameID = findGameID(&g);
	// alrighty, time to open a connection to the game, let's start with the mutex
	sprintf(g.tmpBuf, "%s_mutex", gameID);
	HANDLE mutex = OpenMutexA(MUTEX_ALL_ACCESS, TRUE, g.tmpBuf);
	if (!mutex) {
		internalError(&g, "could not open mutex (game not running/detection failed?)", EM_INIT, 0);
		return;
	}
	// wait to acquire the mutex
	WaitForSingleObject(mutex, INFINITE);
	// Perform stuff protected by the mutex
	handleClientInsideMutex(&g, gameID);
	// Release & close
	ReleaseMutex(mutex);
	CloseHandle(mutex);
}

