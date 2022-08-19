#define SDL_MAIN_HANDLED
#include <SDL2/SDL.h>

int main(int argc, char ** argv) {
	SDL_Window * w;
	SDL_Renderer * r;
	SDL_Init(SDL_INIT_VIDEO);
	SDL_CreateWindowAndRenderer(640, 480, SDL_WINDOW_RESIZABLE, &w, &r);
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

		SDL_RenderPresent(r);
	}
}

