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

// rendering

extern SDL_Window * gWindow;
extern SDL_Renderer * gRenderer;
extern SDL_Texture * gFont;

void writeText(const SDL_Point a, const char * text);
void writeText(const SDL_Point a, const CMSlice & text);
void writeText(const SDL_Point a, const char * text, size_t len);

void fillRect(const SDL_Rect rect, uint32_t colour);
void drawLine(const SDL_Point a, const SDL_Point b, uint32_t colour);

// points

inline static SDL_Point ptOfs(const SDL_Point a, const SDL_Point b) {
	return {a.x + b.x, a.y + b.y};
}

inline static SDL_Point operator +(const SDL_Point a, const SDL_Point b) {
	return {a.x + b.x, a.y + b.y};
}

// rect points

inline static SDL_Point rcUL(const SDL_Rect a) {
	return {a.x, a.y};
}

inline static SDL_Point rcCentre(const SDL_Rect a) {
	return {a.x + (a.w / 2), a.y + (a.h / 2)};
}

// rects

inline static SDL_Rect rcMargin(const SDL_Rect basis, int amount) {
	int a2 = amount * 2;
	return {basis.x + amount, basis.y + amount, basis.w - a2, basis.h - a2};
}

inline static SDL_Rect rcTranslate(const SDL_Rect basis, const SDL_Point move) {
	return {basis.x + move.x, basis.y + move.y, basis.w, basis.h};
}

// controls

class CMControl : public CMObject {
public:

	CMControl() {}
	virtual ~CMControl();

	void setParent(CMControl * par);
	CMControl * getParent() { return _parent; }

	SDL_Rect bounds() {
		return _bounds;
	}
	// run every frame for layout
	virtual void setBounds(const SDL_Rect & rect);

	virtual void onDraw(const SDL_Point & mouseAt);
	// true: click counted as a hit
	virtual bool onClick(const SDL_Point & mouseAt);

	// sizing
	virtual int getHeightForWidth(int width);
	virtual int getWidthForHeight(int height);
	SDL_Point getIdealSize() { return _idealSize; }

	// Don't call in response to setBounds or such!
	void updatedContents(const SDL_Point & size) {
		_idealSize = size;
	}
private:
	SDL_Rect _bounds = {};
	SDL_Point _idealSize = {};
	CMControl * _parent = NULL;
	CMControl * _firstChild = NULL;
	CMControl * _lastChild = NULL;
	CMControl * _prev = NULL;
	CMControl * _next = NULL;
};

