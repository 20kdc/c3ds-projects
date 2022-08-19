/*
 * creature-monitor - watching norns from afar?
 * Written starting in 2022 by 20kdc
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

#include "main.h"

class CMTestState : public CMState {
public:
	void frame() {
		SDL_SetRenderDrawColor(gRenderer, 0, 0, 0, 255);
		SDL_RenderClear(gRenderer);
		writeText(0, 0, "Hello world!");
		SDL_RenderPresent(gRenderer);
	}
	void event(SDL_Event & event) {
	}
};

void setInitialState() {
	gCurrentState = new CMTestState();
}

