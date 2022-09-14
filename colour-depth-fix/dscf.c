/*
 * ragnarok - a device to improve Wine compatibility with Docking Station
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

// ragnarok 4: version 2 & 3 spliced together
// acts as a ddraw hook so we can use the ragnarok 3 injector
// but actually acts like ragnarok 2 because it's more stable

#include <windows.h>
#include <ddraw.h>
#include <stdlib.h>
#include <stdio.h>
#include <stdint.h>

typedef struct {
	uint32_t address;
	const void * target;
	const char * expected;
} hook_t;

typedef struct {
	const hook_t * hooks;
	const char * name;
} suite_t;

// exported from the ASM side
extern const suite_t ddraw_hook_table[];

static int hookValid(const hook_t * hook) {
	const char * expectedChr = hook->expected;
	char * adr = (char *) hook->address;
	for (int i = 0; i < 5; i++)
		if (expectedChr[i] != adr[i])
			return 0;
	return 1;
}

static void hookPatch(const hook_t * hook) {
	const char * expectedChr = (char *) hook->expected;
	char * adr = (char *) hook->address;
	DWORD ignoreMe = 0;
	VirtualProtect((void *) adr, 5, 0x80, &ignoreMe);
	adr[0] = 0xE8;
	*((uint32_t*) (adr + 1)) = ((uint32_t) hook->target) - (hook->address + 5);
	FlushInstructionCache(0, (void *) adr, 5);
}

static HMODULE ddrawModule;

// I keep thinking this is marked WINAPI (stdcall). Be aware: this is NOT stdcall.
__declspec(dllexport) HRESULT DirectDrawHooked(GUID * lpGUID, LPDIRECTDRAW * ddraw, IUnknown * unkOuter) {
	HRESULT (*ddc)(GUID *, LPDIRECTDRAW *, IUnknown *) = (void *) GetProcAddress(ddrawModule, "DirectDrawCreate");
	return ddc(lpGUID, ddraw, unkOuter);
}

static void attemptHooks() {
	const suite_t * hookSuite = ddraw_hook_table;
	fprintf(stderr, "colour-depth-fix: scanning hooksites\n");
	while (hookSuite->hooks) {
		const hook_t * hook = hookSuite->hooks;
		while (hook->address) {
			if (!hookValid(hook)) {
				fprintf(stderr, "colour-depth-fix: engine: %s failed because at hook %x\n", hookSuite->name, hook->address);
				break;
			}
			hook++;
		}
		if (!hook->address) {
			// got to end, so valid!
			fprintf(stderr, "colour-depth-fix: hooksite analysis indicates engine: %s\n", hookSuite->name);
			hook = hookSuite->hooks;
			while (hook->address) {
				hookPatch(hook);
				hook++;
			}
			return;
		}
		hookSuite++;
	}
	MessageBoxA(NULL, "engine.exe not supported, your window title should contain one of:\r\n"
		"Engine 2.286 B195\r\n"
		"", "colour-depth-fix", MB_OK);
}

BOOL WINAPI DllMain(HINSTANCE x, DWORD y, void * z) {
	switch (y)
	{
		case DLL_PROCESS_ATTACH:
			ddrawModule = LoadLibraryA("DDRAW.dll");
			attemptHooks();
			break;
		case DLL_PROCESS_DETACH:
			FreeLibrary(ddrawModule);
			break;
	}
	return TRUE;
}

