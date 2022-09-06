/*
 * caosprox - CPX server reference implementation
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

// GUI module - contains the fancy UI

#include <windows.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>

// returns non-zero on error
extern int cpxservi_serverInit(int host, int port);
extern const char * cpxservi_gameID;
// this uses stdio, so we need to be sure we stop using it from main thread if in UI mode!
extern void cpxservi_serverLoop();

static HWND globalWindow;
static int mbMutex = 0;

static LRESULT WINAPI cpxservg_wp(HWND hwnd, UINT uMsg, WPARAM wParam, LPARAM lParam) {
	if (uMsg == 0x7000) {
		if (lParam == WM_LBUTTONDOWN) {
			if (!mbMutex) {
				mbMutex = 1;
				if (MessageBoxA(NULL, "Do you want to shutdown the CAOS Proxy (CPX) server?\r\nThis may cause applications connected to a Creatures series game via this server to malfunction.", "CAOS Proxy (CPX) Server", MB_YESNO) == IDYES) {
					ExitProcess(0);
				}
				mbMutex = 0;
			}
		}
	}
	return DefWindowProcA(hwnd, uMsg, wParam, lParam);
}

static void cpxservg_ui() {
	// create window class
	WNDCLASSA windowClass = {};
	windowClass.lpfnWndProc = cpxservg_wp;
	windowClass.hInstance = GetModuleHandleA(NULL);
	windowClass.lpszClassName = "CAOSProxServerClass";
	RegisterClassA(&windowClass);

	// create window
	globalWindow = CreateWindowA("CAOSProxServerClass", "CAOSProx", WS_OVERLAPPEDWINDOW, 0, 0, 800, 600, NULL, NULL, NULL, NULL);

	// create notification icon
	NOTIFYICONDATAA notifyIcon = {};
	notifyIcon.cbSize = sizeof(notifyIcon);
	notifyIcon.hWnd = globalWindow;
	notifyIcon.uFlags = NIF_ICON | NIF_TIP | NIF_MESSAGE;
	notifyIcon.uCallbackMessage = 0x7000;
	notifyIcon.hIcon = LoadIconA(GetModuleHandleA(NULL), MAKEINTRESOURCEA(1000));
	strcpy(notifyIcon.szTip, "CPX Server for Creatures 3/DS");
	Shell_NotifyIconA(NIM_ADD, &notifyIcon);

	// standard message loop
	MSG msg = {};
	while (GetMessageA(&msg, NULL, 0, 0) > 0) {
		TranslateMessage(&msg); 
		DispatchMessageA(&msg);
	}
}

static DWORD WINAPI cpxservg_serverThread(void * param) {
	cpxservi_serverLoop();
	return 0;
}

int main(int argc, char ** argv) {
	int port = 19960;
	int mode = 0;
	int host = 0x0100007F;
	// check mode
	// quiet: if we can't initialize, return an error but don't messagebox
	// invisible: don't create notification icon
	if (argc >= 2) {
		if (!strcmp(argv[1], "loud"))
			mode = 0;
		if (!strcmp(argv[1], "quiet"))
			mode = 1;
		if (!strcmp(argv[1], "invisible"))
			mode = 2;
		if (!strcmp(argv[1], "help")) {
			puts("caosprox [loud/quiet/invisible/help] [HOST] [PORT] [game name]");
			puts("default mode is loud, HOST is 127.0.0.1 (set to 0.0.0.0 for remote access), default PORT is 19960, default game is autodetected");
			puts("all prior args must be specified if you wish to use one");
			return 1;
		}
	}
	// remote override
	if (argc >= 3) {
		host = inet_addr(argv[2]);
	}
	// port override
	if (argc >= 4) {
		port = atoi(argv[3]);
	}
	// game override
	if (argc >= 5) {
		cpxservi_gameID = strdup(argv[4]);
	}
	printf("caosprox host %08x port %i mode %i targetting %s\n", host, port, mode, cpxservi_gameID ? cpxservi_gameID : "<autodetect>");
	// setup server stuff immediately so we can bail if there's some obvious issue like two servers
	if (mode == 0) {
		while (cpxservi_serverInit(host, port)) {
			puts("caosprox server init failure");
			int res = MessageBoxA(NULL, "CPX server failed to setup networking - possible port conflict or firewall", "CAOSProx", MB_RETRYCANCEL);
			if (res == IDCANCEL)
				return 1;
		}
	} else if (cpxservi_serverInit(host, port)) {
		// near-silently die
		puts("caosprox server init failure");
		return 1;
	}
	puts("caosprox server init complete");
	if (mode != 2) {
		puts("caosprox about to create server thread");
		CreateThread(NULL, 0, cpxservg_serverThread, NULL, 0, NULL);
		cpxservg_ui();
	} else {
		puts("caosprox about to enter server loop");
		cpxservi_serverLoop();
	}
	return 0;
}

