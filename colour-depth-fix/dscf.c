/*
 * ragnarok - a device to improve Wine compatibility with Docking Station
 * Written starting in 2022 by 20kdc
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

// ragnarok 4: version 2 & 3 spliced together
// acts as a ddraw hook so we can use the ragnarok 3 injector
// but actually acts like ragnarok 2 because it's more stable

#include <windows.h>
#include <ddraw.h>
#include <stdlib.h>
#include <stdint.h>

extern void ddraw_createsurface_hook_ecx_code();
extern void ddraw_createsurface_hook_ecx_test();
#define DDRAW_CREATESURFACE_HOOK_ECX ddraw_createsurface_hook_ecx_code, ddraw_createsurface_hook_ecx_test

extern void ddraw_createsurface_hook_edx_code();
extern void ddraw_createsurface_hook_edx_test();
#define DDRAW_CREATESURFACE_HOOK_EDX ddraw_createsurface_hook_edx_code, ddraw_createsurface_hook_edx_test

static void patchCall(uint32_t address, const void * target, const void * expected) {
	const char * expectedChr = (char *) expected;
	char * adr = (char *) address;
	DWORD ignoreMe = 0;
	VirtualProtect((void *) address, 5, 0x80, &ignoreMe);
	if (expected != 0) {
		for (int i = 0; i < 5; i++) {
			if (expectedChr[i] != adr[i]) {
				MessageBoxA(NULL, "Hook target mismatch - this engine.exe is of the wrong version. Your window title should contain \"Engine 2.286 B195\".", "Ragnarok", MB_OK);
				return;
			}
		}
	}
	adr[0] = 0xE8;
	*((uint32_t*) (adr + 1)) = ((uint32_t) target) - (address + 5);
	FlushInstructionCache(0, (void *) address, 5);
}

static HMODULE ddrawModule;

__declspec(dllexport) HRESULT DirectDrawHooked(GUID * lpGUID, LPDIRECTDRAW * ddraw, IUnknown * unkOuter) {
	HRESULT (*ddc)(GUID *, LPDIRECTDRAW *, IUnknown *) = (void *) GetProcAddress(ddrawModule, "DirectDrawCreate");
	return ddc(lpGUID, ddraw, unkOuter);
}

BOOL WINAPI DllMain(HINSTANCE x, DWORD y, void * z) {
	switch (y)
	{
		case DLL_PROCESS_ATTACH:
			ddrawModule = LoadLibraryA("DDRAW.dll");
			// CreateFullscreenDisplaySurfaces
			patchCall(0x00472FE1, DDRAW_CREATESURFACE_HOOK_ECX);
			patchCall(0x0047304B, DDRAW_CREATESURFACE_HOOK_EDX);
			patchCall(0x00473069, DDRAW_CREATESURFACE_HOOK_EDX);
			// CreateWindowedDisplaySurfaces
			patchCall(0x0047327B, DDRAW_CREATESURFACE_HOOK_ECX);
			patchCall(0x004732BA, DDRAW_CREATESURFACE_HOOK_EDX);
			// FlipScreenHorizontally
			patchCall(0x004737B9, DDRAW_CREATESURFACE_HOOK_EDX);
			patchCall(0x004737D6, DDRAW_CREATESURFACE_HOOK_EDX);
			// CreateSurface
			patchCall(0x0047626E, DDRAW_CREATESURFACE_HOOK_EDX);
			patchCall(0x0047628C, DDRAW_CREATESURFACE_HOOK_EDX);
			break;
		case DLL_PROCESS_DETACH:
			FreeLibrary(ddrawModule);
			break;
	}
	return TRUE;
}

