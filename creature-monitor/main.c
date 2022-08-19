/*
 * creature-monitor - watching norns from afar?
 * Written starting in 2022 by 20kdc
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

#define SDL_MAIN_HANDLED
#include <SDL2/SDL.h>

#include <stdio.h>

extern const char cm_page_start[];
extern const int cm_page_len;

int main(int argc, char ** argv) {
	SDL_Window * w;
	SDL_Renderer * r;
	SDL_Init(SDL_INIT_VIDEO);
	SDL_CreateWindowAndRenderer(640, 480, SDL_WINDOW_RESIZABLE, &w, &r);
	SDL_Surface * s = SDL_LoadBMP_RW(SDL_RWFromConstMem(cm_page_start, cm_page_len), 1);
	if (!s)
		puts("wah! failed to open BMP!");
	puts("opened BMP");
	SDL_Texture * t = SDL_CreateTextureFromSurface(r, s);
	if (!t)
		puts("wah! failed to create texture!");
	puts("created texture");
	while (1) {
		SDL_Event ev;
		while (SDL_PollEvent(&ev)) {
			if (ev.type == SDL_QUIT)
				return 0;
		}
		SDL_Delay(100);
		SDL_SetRenderDrawColor(r, 0, 0, 0, 255);
		SDL_RenderClear(r);
		SDL_SetRenderDrawColor(r, 255, 255, 255, 255);

		SDL_RenderDrawLine(r, 32, 400, 64, 80);
		SDL_RenderDrawLine(r, 0, 160, 128, 160);

		SDL_RenderDrawLine(r, 256, 80, 128, 160);
		SDL_RenderDrawLine(r, 128, 160, 256, 240);
		SDL_RenderDrawLine(r, 256, 240, 128, 400);

		SDL_RenderDrawLine(r, 256, 400, 300, 80);
		SDL_RenderDrawLine(r, 256, 160, 400, 160);

		SDL_RenderCopy(r, t, NULL, NULL);

		SDL_RenderPresent(r);
	}
}

