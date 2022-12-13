/*
 * caosprox - CPX server reference implementation
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

#pragma once

// Common headers between cpxserv stuff (but not the inverter)

#include "libcpx.h"

// main
extern HWND globalWindow;
extern UINT msgTrayBlink;

// cpxservi
extern const char * cpxservi_gameID;
extern const char * cpxservi_gamePath;
void cpxservi_handleClient(libcpx_channel_t * client);

// cpxservl
// returns non-zero on error
int cpxservl_serverInit(int host, int port);
// this uses stdio, so we need to be sure we stop using it from main thread if in UI mode!
void cpxservl_serverLoop();

