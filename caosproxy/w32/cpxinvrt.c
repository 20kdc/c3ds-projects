/*
 * caosprox - CPX server reference implementation
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

// The "inverter" (aka CPX->SHM) is a CPX client that hosts the SHM server. As such it acts as an "inverter" for tools.
// This is useful because it allows running CL tools with the Linux game.

#define _GNU_SOURCE
#include <stdio.h>

#include "libcpx.h"
#include <shlwapi.h>

static int cpxHost = 0x0100007F;
static int cpxPort = 19960;

static SOCKET tryConnect() {
	SOCKET res = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
	if (res == INVALID_SOCKET)
		return res;
	struct sockaddr_in target = {
		.sin_family = AF_INET,
		.sin_addr = {
			.s_addr = cpxHost
		},
		.sin_port = htons(cpxPort),
	};
	if (connect(res, (struct sockaddr *) &target, sizeof(target))) {
		closesocket(res);
		return INVALID_SOCKET;
	}
	return res;
}

// theoretically, we should be getting this from the server
// in practice, REALLY?
// store this as a non-static exported global so this could be changed via binary patching, if anyone cares (they won't)
__declspec(dllexport) int capacityPre = '>PAC';
__declspec(dllexport) int capacity = 1048576;

static HANDLE fileMapping, mutex, resultEvent, requestEvent;
static libcpx_shmHeader_t * shm;

static char * postpend(const char * fmt, const char * addr) {
	char * res = NULL;
	// asprintf is genius technology from future space civilizations
	// still not as convenient as it could be, though.
	if (asprintf(&res, fmt, addr) == -1)
		res = NULL;
	return res;
}

static int createBasicObjects(const char * gameName) {
	char * s1Mem = postpend("%s_mem", gameName);
	char * s2Mtx = postpend("%s_mutex", gameName);
	char * s3Res = postpend("%s_result", gameName);
	char * s4Req = postpend("%s_request", gameName);
	if (!(s1Mem && s2Mtx && s3Res && s4Req)) {
		free(s1Mem);
		free(s2Mtx);
		free(s3Res);
		free(s4Req);
		printf("Unable to allocate game name object name string.\n");
		return 1;
	}
	HANDLE fileMapping = CreateFileMappingA(INVALID_HANDLE_VALUE, NULL, 0x04, 0, sizeof(libcpx_shmHeader_t) + capacity, s1Mem);
	if (!fileMapping) {
		printf("Unable to allocate shared memory.\n");
		return 1;
	}
	// initialize SHM header with some nonsense for now
	shm = (libcpx_shmHeader_t *) MapViewOfFile(fileMapping, FILE_MAP_ALL_ACCESS, 0, 0, 0);
	if (!shm) {
		printf("Unable to map shared memory.\n");
		return 1;
	}
	// we can pretty much forget about this mutex, it's a mutex held by other applications "in our name".
	mutex = CreateMutexA(NULL, FALSE, s2Mtx);
	resultEvent = CreateEventA(NULL, TRUE, FALSE, s3Res);
	requestEvent = CreateEventA(NULL, TRUE, FALSE, s4Req);
	return 0;
}

static void setupRegistryKeys(const char * gameName, const char * gamePath) {
	SHSetValueA(HKEY_CURRENT_USER, "Software\\CyberLife Technology\\Creatures Engine", "Default Game", REG_SZ, gameName, strlen(gameName) + 1);
	char * subKey = postpend("Software\\CyberLife Technology\\%s", gameName);
	if (subKey) {
		char * keys[] = {
			"Backgrounds Directory", "%s\\Backgrounds\\",
			"Body Data Directory", "%s\\Body Data\\",
			"Bootstrap Directory", "%s\\Bootstrap\\",
			"Catalogue Directory", "%s\\Catalogue\\",
			"Creature Galleries Directory", "%s\\Creature Galleries\\",
			"Exported Creatures Directory", "%s\\Exported Creatures\\",
			"Genetics Directory", "%s\\Genetics\\",
			"Images Directory", "%s\\Images\\",
			"Journal Directory", "%s\\Journal\\",
			"Main Directory", "%s\\",
			"Overlay Data Directory", "%s\\Overlay Data\\",
			"Resource Files Directory", "%s\\My Agents\\",
			"Sounds Directory", "%s\\Sounds\\",
			"Users Directory", "%s\\Users\\",
			"Worlds Directory", "%s\\My Worlds\\",
			NULL
		};
		for (int i = 0; keys[i]; i += 2) {
			char * res = postpend(keys[i + 1], gamePath);
			if (res) {
				// now that's what I call Byzantine:
				// SHSetValueA == RegSetKeyValueA but the latter is a versioning risk
				SHSetValueA(HKEY_LOCAL_MACHINE, subKey, keys[i], REG_SZ, res, strlen(res) + 1);
				free(res);
			} else {
				printf("didn't manage to perform postpend for %s???\n", keys[i]);
			}
		}
		free(subKey);
	} else {
		printf("didn't manage to perform postpend of LM subKey???\n");
	}
}

static void reportError(const char * err) {
	shm->resultCode = 1;
	shm->sizeBytes = strlen(err) + 1;
	strcpy(shm->data, err);
}

static void initShmBase() {
	// we do this before every request - it shouldn't really matter
	memcpy(shm->magic, "c2e@", 4);
	shm->pid = GetCurrentProcessId();
	shm->maxSizeBytes = capacity;
}

int main(int argc, char ** argv) {
	printf("CAOS Proxy Inverter / CPX->SHM, version " LIBCPX_VERSION "\n");
	if ((argc < 2) || (argc > 4)) {
		printf("cpxinvrt GAMEPATH [HOST [PORT]]\n");
		printf("HOST must be an IP address for now (default 127.0.0.1)\n");
		printf("game path is used to generate the registry key for the CPXSHM game\n");
		return 1;
	}
	if (argc >= 3)
		cpxHost = inet_addr(argv[2]);
	if (argc >= 4)
		cpxPort = atoi(argv[3]);
	libcpx_initWinsock();
	printf("Creating basics...\n");
	if (createBasicObjects("CPXSHM"))
		return 1;
	printf("Creating registry keys...\n");
	setupRegistryKeys("CPXSHM", argv[1]);
	printf("Initializing SHM contents...\n");
	initShmBase();
	printf("Ready.\n");
	while (1) {
		// continue forth
		WaitForSingleObject(requestEvent, INFINITE);
		SOCKET cpx = tryConnect();
		if (!cpx) {
			reportError("CPXSHM: Unable to connect to CPX server.");
		} else {
			libcpx_shmHeader_t tmp;
			// get initial header (to ignore)
			if (libcpx_sgeta(cpx, &tmp, sizeof(libcpx_shmHeader_t)) != sizeof(libcpx_shmHeader_t)) {
				reportError("CPXSHM: CPX server connection failed or did not send initial header.");
				goto closeSocketAndFinish;
			}
			// send request
			int reqLen = strlen(shm->data) + 1;
			libcpx_sputa(cpx, &reqLen, 4);
			libcpx_sputa(cpx, shm->data, reqLen);
			// read response
			if (libcpx_sgeta(cpx, &tmp, sizeof(libcpx_shmHeader_t)) != sizeof(libcpx_shmHeader_t)) {
				reportError("CPXSHM: CPX server did not send a response.");
				goto closeSocketAndFinish;
			}
			shm->resultCode = tmp.resultCode;
			shm->sizeBytes = tmp.sizeBytes;
			libcpx_sgeta(cpx, shm->data, tmp.sizeBytes);
			// we're done!
			closeSocketAndFinish:
			closesocket(cpx);
		}
		// refresh this
		initShmBase();
		ResetEvent(requestEvent);
		PulseEvent(resultEvent);
	}
}

