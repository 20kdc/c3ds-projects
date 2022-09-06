/*
 * creature-monitor - watching norns from afar?
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

#include "ui_ctrl.h"
#include <stdio.h>

SDL_Window * gWindow;
SDL_Renderer * gRenderer;
SDL_Texture * gFont;

// fun

void cmPanic(const char * reason) {
	fprintf(stderr, "panic: %s\n", reason);
	SDL_ShowSimpleMessageBox(SDL_MESSAGEBOX_ERROR, "Error!", reason, gWindow);
	abort();
}

// rendering core

void writeText(const SDL_Point a, const char * text) {
	writeText(a, text, strlen(text));
}

void writeText(const SDL_Point a, const CMSlice & textSlice) {
	writeText(a, textSlice.data, textSlice.length);
}

void writeText(const SDL_Point a, const char * text, size_t len) {
	SDL_Rect src = {0, 0, 7, 14};
	SDL_Rect dst = {a.x, a.y, 7, 14};
	while (len) {
		len--;
		char ch = *text++;
		if (ch == 10) {
			dst.x = a.x;
			dst.y += 16;
		} else {
			src.x = (ch & 0x0F) * 7;
			src.y = ((ch & 0xF0) >> 4) * 14;
			SDL_RenderCopy(gRenderer, gFont, &src, &dst);
			dst.x += 8;
		}
	}
}

static void setDrawColour(uint32_t colour) {
	SDL_SetRenderDrawColor(gRenderer, (colour >> 16) & 0xFF, (colour >> 8) & 0xFF, colour & 0xFF, (colour >> 24) & 0xFF);
}

void fillRect(const SDL_Rect rect, uint32_t colour) {
	setDrawColour(colour);
	SDL_RenderFillRect(gRenderer, &rect);
}

void drawLine(const SDL_Point a, const SDL_Point b, uint32_t colour) {
	setDrawColour(colour);
	SDL_RenderDrawLine(gRenderer, a.x, a.y, b.x, b.y);
}

// control/panel

CMControl::~CMControl() {
	setParent(NULL);
	while (_firstChild) {
		// causes setParent(NULL)
		delete _firstChild;
	}
}

void CMControl::setParent(CMControl * parent) {
	if (_parent) {
		if (_next) {
			_next->_prev = _prev;
		} else {
			_parent->_lastChild = _prev;
		}
		if (_prev) {
			_prev->_next = _next;
		} else {
			_parent->_firstChild = _next;
		}
		// finish unlinking
		_next = NULL;
		_prev = NULL;
		_parent = NULL;
	}
	_parent = parent;
	if (_parent) {
		if (!_parent->_lastChild) {
			// we're the first one!
			_parent->_firstChild = this;
			_parent->_lastChild = this;
		} else {
			// we're not the first one, link in at the end
			_prev = _parent->_lastChild;
			_prev->_next = this;
			_parent->_lastChild = this;
		}
	}
}

void CMControl::setBounds(const SDL_Rect & rect) {
	_bounds = rect;
}

void CMControl::onDraw(const SDL_Point & mouseAt) {
	CMControl * iter = _firstChild;
	while (iter) {
		iter->onDraw(mouseAt);
		iter = iter->_next;
	}
}

bool CMControl::onClick(const SDL_Point & mouseAt) {
	CMControl * iter = _lastChild;
	while (iter) {
		if (iter->onClick(mouseAt))
			return true;
		iter = iter->_prev;
	}
	return false;
}

void CMControl::onUpNotify(int id) {
	if (_parent)
		_parent->onUpNotify(id);
}

int CMControl::getHeightForWidth(int width) {
	return _idealSize.y;
}
int CMControl::getWidthForHeight(int height) {
	return _idealSize.x;
}

static SDL_Point measureText(const CMSlice & txt) {
	SDL_Point emuWriter = {0, 16};
	SDL_Point totalNecessary = {0, 16};
	for (int i = 0; i < txt.length; i++) {
		char ch = txt.data[i];
		if (ch == 10) {
			emuWriter.x = 0;
			emuWriter.y += 16;
		} else {
			emuWriter.x += 8;
		}
		// max X/Y
		if (totalNecessary.x < emuWriter.x)
			totalNecessary.x = emuWriter.x;
		if (totalNecessary.y < emuWriter.y)
			totalNecessary.y = emuWriter.y;
	}
	return totalNecessary;
}

// label/button

void CMText::setText(const CMSlice & txt) {
	text = txt;
	updatedContents(measureText(txt));
}
void CMText::onDraw(const SDL_Point & mouseAt) {
	SDL_Rect b = bounds();
	if (rcContains(b, mouseAt)) {
		fillRect(b, 0xFF000040);
	}
	writeText({b.x, b.y}, text);
}
bool CMText::onClick(const SDL_Point & mouseAt) {
	SDL_Rect b = bounds();
	if (rcContains(b, mouseAt)) {
		onUpNotify(notifyId);
		return true;
	}
	return false;
}

// margin

CMMargin::CMMargin(CMControl * innards, int m) : content(innards), margin(m) {
	innards->setParent(this);
}

void CMMargin::setBounds(const SDL_Rect & rect) {
	CMControl::setBounds(rect);
	content->setBounds(rcMargin(rect, margin));
}

