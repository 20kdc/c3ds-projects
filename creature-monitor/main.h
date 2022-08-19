/*
 * creature-monitor - watching norns from afar?
 * Written starting in 2022 by 20kdc
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

#pragma once

#define SDL_MAIN_HANDLED
#include <SDL2/SDL.h>

extern SDL_Window * gWindow;
extern SDL_Renderer * gRenderer;
extern SDL_Texture * gFont;

void errorOut(const char * reason);
void writeText(int x, int y, const char * text);

class CMObject {
public:
	virtual ~CMObject() {}
	void queueDelete();
	CMObject * _nextInDeleteQueue;
};

class CMState : public CMObject {
public:
	virtual void frame() = 0;
	virtual void event(SDL_Event & event) = 0;
};

extern CMState * gCurrentState;
extern CMObject * gQueuedDelete;

extern void setInitialState();

