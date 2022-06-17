/*
 * caosprox - CPX server reference implementation
 * Written starting in 2022 by 20kdc
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

// With extreme thanks to http://double.nz/creatures/developer/sharedmemory.htm

// Internals module - contains the actual server
#include <stdlib.h>
#include <stdio.h>
#include <winsock2.h>
#include <windows.h>

static SOCKET serverSocket;
// Initialize
int cpxservi_serverInit(int host, int port) {
	// deal with WS nonsense
	WSADATA dontcare = {};
	WSAStartup(MAKEWORD(2, 2), &dontcare);
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

const char * cpxservi_gameID = NULL;

static int sgetc(SOCKET s) {
	char chr;
	if (recv(s, &chr, 1, 0) == 1)
		return chr & 0xFF;
	return -1;
}

static int sgeta(SOCKET s, void * target, int len) {
	char * targetC = target;
	for (int i = 0; i < len; i++) {
		int res = sgetc(s);
		if (res == -1)
			return i;
		targetC[i] = res;
	}
	return len;
}

static void sputc(SOCKET s, char chr) {
	send(s, &chr, 1, 0);
}

static void sputa(SOCKET s, const void * target, int len) {
	const char * targetC = target;
	for (int i = 0; i < len; i++)
		sputc(s, targetC[i]);
}


static char gameIDBuffer[8192];
static char tmpBuffer[8200]; // above + 8 chars for _request

static const char * findGameID() {
	if (cpxservi_gameID)
		return cpxservi_gameID;
	HKEY key;
	if (!RegOpenKeyA(HKEY_CURRENT_USER, "Software\\CyberLife Technology\\Creatures Engine", &key)) {
		LONG gameIDBufferLen = 8191;
		int result = RegQueryValueA(key, "Default Game", gameIDBuffer, &gameIDBufferLen);
		RegCloseKey(key);
		if (!result) {
			// only try this if it succeeds - ERROR_MORE_DATA will cause it to run off the end of the buffer
			gameIDBuffer[gameIDBufferLen] = 0;
			return gameIDBuffer;
		}
	}
	return "Docking Station";
}

typedef struct {
	char magic[4];
	DWORD pid;
	int resultCode, sizeBytes, maxSizeBytes, padding;
	char data[];
} transfer_t;

static void transferAreaToClient(SOCKET client, const transfer_t * area, int hdrOnly) {
	sputa(client, area, hdrOnly ? 24 : (area->sizeBytes + 24));
}

// Note! doubleSend is 1 if we haven't sent the initial 24-byte header.
static void internalError(SOCKET client, const char * text, int doubleSend) {
	transfer_t * tmpErr = (transfer_t *) tmpBuffer;
	memcpy(tmpErr->magic, "c2e@", 4);
	sprintf(tmpErr->data, "caosprox: %s", text);
	tmpErr->pid = 0;
	tmpErr->resultCode = 1;
	tmpErr->sizeBytes = strlen(tmpErr->data) + 1;
	tmpErr->maxSizeBytes = 8192;
	tmpErr->padding = 0;
	if (doubleSend)
		transferAreaToClient(client, tmpErr, 1);
	transferAreaToClient(client, tmpErr, 0);
}

static void handleClientWithEverything(SOCKET client, transfer_t * shm, HANDLE resultEvent, HANDLE requestEvent, HANDLE process) {
	// send SHM state to client
	transferAreaToClient(client, shm, 1);
	// now we want a size back
	int size;
	if (sgeta(client, &size, 4) != 4) {
		internalError(client, "failed to get request size", 0);
		return;
	}
	if (size > shm->maxSizeBytes) {
		internalError(client, "request size exceeds maximum size", 0);
		return;
	}
	// can't hurt
	shm->sizeBytes = size;
	if (sgeta(client, shm->data, size) != size) {
		internalError(client, "failed to get request body", 0);
		return;
	}
	// actually run the request
	ResetEvent(resultEvent);
	PulseEvent(requestEvent);
	HANDLE waitHandles[2] = {process, resultEvent};
	WaitForMultipleObjects(2, waitHandles, FALSE, INFINITE);
	// send the results back to the client
	transferAreaToClient(client, shm, 0);
}

static void handleClientWithSHM(SOCKET client, const char * gameID, transfer_t * shm) {
	HANDLE process = OpenProcess(PROCESS_ALL_ACCESS, FALSE, shm->pid);
	if (!process) {
		internalError(client, "failed to open process handle (game dead?)", 1);
		return;
	}
	sprintf(tmpBuffer, "%s_result", gameID);
	HANDLE resultEvent = OpenEventA(EVENT_ALL_ACCESS, FALSE, tmpBuffer);
	if (!resultEvent) {
		internalError(client, "failed to open result handle", 1);
		CloseHandle(process);
		return;
	}
	sprintf(tmpBuffer, "%s_request", gameID);
	HANDLE requestEvent = OpenEventA(EVENT_ALL_ACCESS, FALSE, tmpBuffer);
	if (!requestEvent) {
		internalError(client, "failed to open request handle", 1);
		CloseHandle(resultEvent);
		CloseHandle(process);
		return;
	}
	handleClientWithEverything(client, shm, resultEvent, requestEvent, process);
	CloseHandle(requestEvent);
	CloseHandle(resultEvent);
}

static void handleClientInsideMutex(SOCKET client, const char * gameID) {
	sprintf(tmpBuffer, "%s_mem", gameID);
	HANDLE fma = OpenFileMappingA(FILE_MAP_ALL_ACCESS, FALSE, tmpBuffer);
	if (!fma) {
		internalError(client, "could not open memory handle (game not running/detection failed?)", 1);
		return;
	}
	transfer_t * shm = (transfer_t *) MapViewOfFile(fma, FILE_MAP_ALL_ACCESS, 0, 0, 0);
	if (!shm) {
		internalError(client, "could not map view of shared memory", 1);
		CloseHandle(fma);
		return;
	}
	handleClientWithSHM(client, gameID, shm);
	UnmapViewOfFile(shm);
	CloseHandle(fma);
}

static void handleClient(SOCKET client) {
	const char * gameID = findGameID();
	// alrighty, time to open a connection to the game, let's start with the mutex
	sprintf(tmpBuffer, "%s_mutex", gameID);
	HANDLE mutex = OpenMutexA(MUTEX_ALL_ACCESS, TRUE, tmpBuffer);
	if (!mutex) {
		internalError(client, "could not open mutex (game not running/detection failed?)", 1);
		return;
	}
	// wait to acquire the mutex
	WaitForSingleObject(mutex, INFINITE);
	// Perform stuff protected by the mutex
	handleClientInsideMutex(client, gameID);
	// Release & close
	ReleaseMutex(mutex);
	CloseHandle(mutex);
}

// Main loop
void cpxservi_serverLoop() {
	while (1) {
		// get a client
		SOCKET client = accept(serverSocket, NULL, NULL);
		if (client == INVALID_SOCKET)
			continue;
		handleClient(client);
		// and now we're done
		closesocket(client);
	}
}

