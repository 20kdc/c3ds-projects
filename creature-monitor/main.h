/*
 * creature-monitor - watching norns from afar?
 * Written starting in 2022 by 20kdc
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

#pragma once

#define SDL_MAIN_HANDLED
#include <SDL2/SDL.h>
#include <SDL2/SDL_net.h>

#include "libcm.h"

extern SDL_Window * gWindow;
extern SDL_Renderer * gRenderer;
extern SDL_Texture * gFont;

void errorOut(const char * reason);
void writeText(int x, int y, const char * text);
void writeText(int x, int y, const CMSlice & text);
void writeText(int x, int y, const char * text, size_t len);
void fillRect(const SDL_Rect rect, uint32_t colour);
void drawLine(const SDL_Point a, const SDL_Point b, uint32_t colour);

inline static SDL_Point rectCentre(const SDL_Rect a) {
	return {a.x + (a.w / 2), a.y + (a.h / 2)};
}

inline static SDL_Rect marginRect(const SDL_Rect basis, int amount) {
	int a2 = amount * 2;
	return {basis.x + amount, basis.y + amount, basis.w - a2, basis.h - a2};
}

class CMPeriodic {
public:
	Uint32 nextCheck;
	Uint32 timeBetween;
	CMPeriodic(Uint32 tb) {
		nextCheck = SDL_GetTicks();
		timeBetween = tb;
	}
	bool shouldRun() {
		Uint32 now = SDL_GetTicks();
		if (nextCheck <= now) {
			nextCheck += timeBetween;
			if (nextCheck < now)
				nextCheck = now + timeBetween;
			return true;
		}
		return false;
	}
};

class CMState : public CMObject {
public:
	virtual const char * stateName() = 0;
	virtual void frame(int w, int h) = 0;
	virtual void event(int w, int h, SDL_Event & event) = 0;
};

void setState(CMState * state);

// application stuff
extern CMSlice cmChemicalNames[256];

void setInitialState();
void setSelectorState();
void setChemState(const CMSlice & moniker);
void setBrainState(const CMSlice & moniker);

