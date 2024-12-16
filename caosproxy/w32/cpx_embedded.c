/*
 * caosprox - CPX server reference implementation
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

// Embeddable CPX server module for engines on Wine

#include "cpxservc.h"

void cpxservg_activity() {
}

static DWORD WINAPI cpxservg_serverThread(void * param) {
	cpxservl_serverLoop();
	return 0;
}

BOOL WINAPI DllMain(HINSTANCE h, DWORD r, void *) {
	if (r == DLL_PROCESS_ATTACH) {
		cpxservl_serverInit(0x0100007F, 19960);
		CreateThread(NULL, 0, cpxservg_serverThread, NULL, 0, NULL);
	}
	return TRUE;
}

int CreaturesEngineModuleInterfaceVersion() {
	return 6;
}

typedef struct {
	__thiscall void (*name)(void *, void *);
	__thiscall int (*version)(void *);
	__thiscall void (*caos)(void *, void *);
	__thiscall void (*shutdown)(void *);
	__thiscall void (*update)(void *, int);
	__thiscall void (*world_end)(void *);
	__thiscall void (*world_load)(void *);
	__thiscall char (*persistent)(void *);
	__thiscall char (*write)(void *, void *);
	__thiscall char (*read)(void *, void *);
	__thiscall void (*twin)(void *, void *, void *);
	__thiscall char (*network)(void *);
} c2e_module_vtbl_t;

typedef struct {
	const c2e_module_vtbl_t * vtbl;
} c2e_module_t;

static __thiscall void mi_v1(void * a) {
}
static __thiscall void mi_v2(void * a, void * b) {
}
static __thiscall void mi_v3(void * a, void * b, void * c) {
}

static __thiscall void mi_update(void * a, int t) {
}
static __thiscall int mi_version(void * a) {
	return 1;
}
static __thiscall char mi_false(void * a) {
	return 0;
}
static __thiscall char mi_serializer(void * a, void * b) {
	return 1;
}

static const c2e_module_vtbl_t my_module_vtbl = {
	.name = mi_v2,
	.version = mi_version,
	.caos = mi_v2,
	.shutdown = mi_v1,
	.update = mi_update,
	.world_end = mi_v1,
	.world_load = mi_v1,
	.persistent = mi_false,
	.write = mi_serializer,
	.read = mi_serializer,
	.twin = mi_v3,
	.network = mi_false
};

static const c2e_module_t my_module = {
	.vtbl = &my_module_vtbl
};

const c2e_module_t * CreaturesEngineModuleInterface() {
	return &my_module;
}
