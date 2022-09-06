/*
 * creature-monitor - watching norns from afar?
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

#pragma once

#include "ui_ctrl.h"

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

class CMState : public CMControl {
public:
	virtual const char * stateName();
	virtual void onKeyDown(int sym);
	virtual void setBounds(const SDL_Rect & rect);
	CMControl * fill = NULL;
};

void setState(CMState * state);

// application stuff
extern CMSlice cmChemicalNames[256];

void setInitialState();
void setSelectorState();
void setChemState(const CMSlice & moniker);
void setBrainState(const CMSlice & moniker);

