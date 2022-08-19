/*
 * creature-monitor - watching norns from afar?
 * Written starting in 2022 by 20kdc
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

#include "main.h"
#include <stdio.h>

class CMTestState : public CMState {
public:
	Uint32 nextCheck = 0;

	void frame() {
		SDL_SetRenderDrawColor(gRenderer, 0, 0, 0, 255);
		SDL_RenderClear(gRenderer);
		writeText(0, 0, "Hello world!");
		SDL_RenderPresent(gRenderer);

		Uint32 currentTicks = SDL_GetTicks();
		if (currentTicks > nextCheck) {
			nextCheck = currentTicks + 1000;
			IPaddress ipa = {SDL_SwapBE32(INADDR_LOOPBACK), 19960};
			TCPsocket socket = SDLNet_TCP_Open(&ipa);
			if (socket) {
				puts("opened socket!");
				SDLNet_TCP_Close(socket);
			} else {
				puts("did not open socket!");
			}
		}
	}
	void event(SDL_Event & event) {
	}
};

void setInitialState() {
	gCurrentState = new CMTestState();
}

