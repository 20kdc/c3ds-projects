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
#define DirectDrawCreate THROWAWAYSYMBOL
#include <ddraw.h>
#undef DirectDrawCreate
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

extern const char enforce_window_xy;
extern const char april_fools_24[];

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
__declspec(dllexport) HRESULT DirectDrawCreate(GUID * lpGUID, LPDIRECTDRAW * ddraw, IUnknown * unkOuter) {
	HRESULT (*ddc)(GUID *, LPDIRECTDRAW *, IUnknown *) = (void *) GetProcAddress(ddrawModule, "DirectDrawCreate");
	return ddc(lpGUID, ddraw, unkOuter);
}

// Compatibility with the older hook installer
__declspec(dllexport) HRESULT DirectDrawHooked(GUID * lpGUID, LPDIRECTDRAW * ddraw, IUnknown * unkOuter) {
	return DirectDrawCreate(lpGUID, ddraw, unkOuter);
}

static void aprilFools24() {
	char a = april_fools_24[0];
	char b = april_fools_24[1];
	char c = april_fools_24[2];
	char d = april_fools_24[3];
	// such security, much wow
	if (a == '1' && b == '2' && c == '3' && d == '4') {
		// As this is an April Fool's joke being prepared in advance, the default value does nothing.
		// It will remain doing nothing, too.
		return;
	} else if (a == '2' && b == '8' && c == '4' && d == '5') {
		return;
	} else if (a == '1' && b == '9' && c == '4' && d == '7') {
		return;
	} else if (a == '1' && b == '9' && c == '5' && d == '7') {
		// Someone could guess this by sheer luck.
		return;
	} else if (a == '5' && b == '6' && c == '7' && d == '8') {
		MessageBoxA(NULL, "Thank you for registering!\r\n"
			"To disable this second annoying generic message box, please change the registration code to 1234.\r\n"
			"Thank you for participating in the Awful Registration Experience."
			"", "colour-depth-fix", MB_OK);
		return;
	}
	MessageBoxA(NULL, "ddrhk.dll has been deregistered!\r\n"
		"To register ddrhk.dll, please do one of the following:\r\n"
		" * Open ddrhk.dll in a hex editor, then locate the first instance of the text 'RegCode:XXXX', then overwrite the XXXX with the registration code found in the README supplied with your copy of DDRHK.DLL.\r\n"
		" * Look at this annoying generic message box on each start."
		"", "colour-depth-fix", MB_OK);
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
		"Engine 1.162\r\n"
		"Engine 1.158\r\n"
		"Engine 1.147\r\n"
		"", "colour-depth-fix", MB_OK);
}

BOOL WINAPI DllMain(HINSTANCE x, DWORD y, void * z) {
	switch (y)
	{
		case DLL_PROCESS_ATTACH:
			ddrawModule = LoadLibraryA("DDRAW.dll");
			aprilFools24();
			attemptHooks();
			break;
		case DLL_PROCESS_DETACH:
			FreeLibrary(ddrawModule);
			break;
	}
	return TRUE;
}

void WINAPI specialFixWindowRect(RECT * rect) {
	// convert to a sensible system
	int x = rect->left;
	int y = rect->top;
	int w = rect->right - x;
	int h = rect->bottom - y;
	// if the window's size is too small (which crashes the game), enforce 806x625 (the default)
	// the size that gets set usually to cause this is 160x24, caused by a minimized window
	// however, some heights higher than 24 still crash
	// as it is, if the window is smaller than the minimum you could resize it to legitimately in Wine anyway, reset it
	if (w < 160 || h < 64) {
		w = 806;
		h = 625;
	}
	// if this causes issues with people's multi-monitor setups, I've provided a quick way to turn this off via hex editing
	if (enforce_window_xy == 'Y') {
		// make sure there's some room for the window to be "at least a bit" on-screen
		// like it doesn't have to be VERY on-screen but if it's completely inaccessible that sucks, mmkay?
		int sw = GetSystemMetrics(SM_CXSCREEN) - 32;
		int sh = GetSystemMetrics(SM_CYSCREEN) - 32;
		// if these would push the window off-screen by themselves, don't (X=0/Y=0 is always valid)
		if (sw < 0)
			sw = 0;
		if (sh < 0)
			sh = 0;
		if (x < 0)
			x = 0;
		if (y < 0)
			y = 0;
		if (x > sw)
			x = sw;
		if (y > sh)
			y = sh;
	}
	// writeback
	rect->left = x;
	rect->top = y;
	rect->right = x + w;
	rect->bottom = y + h;
}

