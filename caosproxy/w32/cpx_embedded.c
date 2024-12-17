/*
 * caosprox - CPX server reference implementation
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

// Embeddable CPX server module for engines on Wine

#include "cpxservc.h"
#include <stdint.h>
#include <stdlib.h>

// super-hyper-undocumented stuff only 'documented' by a blogpost on someone's blog posing ABOUT HOW YOU SHOULD NOT USE UNDOCUMENTED STUFF
// you can't make this up, I swear
extern void * __ImageBase;

static HWND globalWindow = NULL;
static UINT msgTrayCallback = WM_USER;
static UINT msgTrayBlink = WM_USER + 1;

void cpxservg_activity() {
	if (globalWindow)
		PostMessageA(globalWindow, msgTrayBlink, 1, 0);
}

static int mbMutex = 0;
static NOTIFYICONDATAA notifyIcon = {};
static NOTIFYICONDATAA notifyIconBlink = {};

static LRESULT WINAPI cpxservg_wp(HWND hwnd, UINT uMsg, WPARAM wParam, LPARAM lParam) {
	if (uMsg == msgTrayCallback) { // system tray callback
		// do nothing
	} else if (uMsg == msgTrayBlink) { // blink tray icon, from network thread
		Shell_NotifyIconA(NIM_MODIFY, &notifyIconBlink);
		SetTimer(globalWindow, msgTrayBlink, 250, NULL);
	} else if (uMsg == WM_TIMER && wParam == msgTrayBlink) { // timer for ending blink finished
		Shell_NotifyIconA(NIM_MODIFY, &notifyIcon);
	}
	return DefWindowProcA(hwnd, uMsg, wParam, lParam);
}

static DWORD WINAPI cpxservg_uiThread(void * param) {
	HMODULE me = (HMODULE) &__ImageBase;
	// create window class
	WNDCLASSA windowClass = {};
	windowClass.lpfnWndProc = cpxservg_wp;
	windowClass.hInstance = GetModuleHandleA(NULL);
	windowClass.lpszClassName = "CAOSProxServerClass";
	RegisterClassA(&windowClass);

	// create window
	globalWindow = CreateWindowA("CAOSProxServerClass", "CAOSProx", WS_OVERLAPPEDWINDOW, 0, 0, 800, 600, NULL, NULL, NULL, NULL);

	// create notification icon(s)
	notifyIcon.cbSize = sizeof(notifyIcon);
	notifyIcon.hWnd = globalWindow;
	notifyIcon.uFlags = NIF_ICON | NIF_TIP | NIF_MESSAGE;
	notifyIcon.uCallbackMessage = msgTrayCallback;
	notifyIcon.hIcon = LoadIconA(me, MAKEINTRESOURCEA(1000));
	notifyIcon.uID = 1;
	strcpy(notifyIcon.szTip, "CPX Server [Embedded]");

	notifyIconBlink = notifyIcon;
	notifyIconBlink.hIcon = LoadIconA(me, MAKEINTRESOURCEA(1001));

	Shell_NotifyIconA(NIM_ADD, &notifyIcon);

	// standard message loop
	MSG msg = {};
	while (GetMessageA(&msg, NULL, 0, 0) > 0) {
		TranslateMessage(&msg);
		DispatchMessageA(&msg);
	}
}

static DWORD WINAPI cpxservg_serverThread(void * param) {
	cpxservl_serverLoop();
	return 0;
}

BOOL WINAPI DllMain(HINSTANCE h, DWORD r, void *) {
	if (r == DLL_PROCESS_ATTACH) {
		CreateThread(NULL, 0, cpxservg_uiThread, NULL, 0, NULL);
		cpxservl_serverInit(0x0100007F, 19960);
		CreateThread(NULL, 0, cpxservg_serverThread, NULL, 0, NULL);
	}
	return TRUE;
}

int CreaturesEngineModuleInterfaceVersion() {
	return 6;
}

typedef struct {
	int allocator;
	char * data;
	size_t len;
	size_t res;
} std_string_t;

static uint8_t * std_string_refcount(std_string_t * str) {
	return (uint8_t *) str->data - 1;
}

static void std_string_unref(std_string_t * str) {
	if (str->data) {
		uint8_t refcount = *std_string_refcount(str);
		if (refcount == 0 || refcount == 255) {
			free(std_string_refcount(str));
		} else {
			refcount--;
			*std_string_refcount(str) = refcount;
		}
	}
}

typedef struct {
	__thiscall void (*name)(void *, std_string_t *);
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
static __thiscall void mi_name(void * t, std_string_t * a) {
	std_string_unref(a);
	char * data = (char *) malloc(5);
	data[0] = (char) 255;
	data[1] = 'c';
	data[2] = 'p';
	data[3] = 'x';
	data[4] = 0;
	a->data = data + 1;
	a->len = 3;
	a->res = 3;
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
	.name = mi_name,
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
