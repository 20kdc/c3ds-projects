/*
 * creature-monitor - watching norns from afar?
 * Written starting in 2022 by 20kdc
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

#include "main.h"

#include <stdio.h>

extern const char cm_page_start[];
extern const int cm_page_len;

CMState * gCurrentState;
void setState(CMState * state) {
	if (gCurrentState != NULL)
		gCurrentState->queueDelete();
	printf("switching state to: %s\n", state->stateName());
	gCurrentState = state;
}

const char * CMState::stateName() {
	return "(unnamed state)";
}

void CMState::onKeyDown(int sym) {
}

void CMState::setBounds(const SDL_Rect & rect) {
	CMControl::setBounds(rect);
	if (fill)
		fill->setBounds(rect);
}

int main(int argc, char ** argv) {
	SDL_Init(SDL_INIT_VIDEO);
	SDLNet_Init();

	if (SDL_CreateWindowAndRenderer(640, 480, SDL_WINDOW_RESIZABLE, &gWindow, &gRenderer))
		cmPanic("wah! failed to create window/renderer!");

	SDL_Surface * s = SDL_LoadBMP_RW(SDL_RWFromConstMem(cm_page_start, cm_page_len), 1);
	if (!s)
		cmPanic("wah! failed to open BMP!");
	puts("opened BMP");

	gFont = SDL_CreateTextureFromSurface(gRenderer, s);
	if (!gFont)
		cmPanic("wah! failed to create texture!");
	puts("created texture");

	setInitialState();
	SDL_Point currentMousePosition = {};
	while (1) {
		SDL_Delay(50);
		SDL_Event ev;
		while (SDL_PollEvent(&ev)) {
			if (ev.type == SDL_QUIT) {
				return 0;
			} else if (ev.type == SDL_MOUSEBUTTONDOWN) {
				currentMousePosition = {ev.button.x, ev.button.y};
				gCurrentState->onClick(currentMousePosition);
			} else if (ev.type == SDL_MOUSEMOTION) {
				currentMousePosition = {ev.motion.x, ev.motion.y};
			} else if (ev.type == SDL_KEYDOWN) {
				gCurrentState->onKeyDown(ev.key.keysym.sym);
			}
		}
		SDL_SetRenderDrawColor(gRenderer, 0, 0, 0, 255);
		SDL_RenderClear(gRenderer);
		// not even going to try to optimize the layout code?
		int w, h;
		SDL_GetWindowSize(gWindow, &w, &h);
		gCurrentState->setBounds({0, 0, w, h});
		gCurrentState->onDraw(currentMousePosition);
		SDL_RenderPresent(gRenderer);
		CMObject::performQueuedDeletions();
	}
}

