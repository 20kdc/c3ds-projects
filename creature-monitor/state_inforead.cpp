/*
 * creature-monitor - watching norns from afar?
 * Written starting in 2022 by 20kdc
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

#include "main.h"
#include "cpx.h"

#include <stdio.h>

class CMInforeadState : public CMState {
public:
	Uint32 nextCheck = 0;
	CPXRequestResult * result = NULL;
	int selectedLine = -1;

	~CMInforeadState() {
		delete result;
	}

	const char * stateName() { return "inforead"; }

	void frame(int w, int h) {
		if (result) {
			writeText(0, 0, result->content.data, result->content.length);
		}

		Uint32 currentTicks = SDL_GetTicks();
		if (currentTicks > nextCheck) {
			nextCheck = currentTicks + 1000;

			if (result)
				delete result;

			// do CPX request to locate them Norns
			result = cpxMakeRawRequest(
				"execute\n"
				"outs \"Boop!\"\n"
			);
		}
		// TODO debug holdover
		setSelectorState();
	}
	void event(int w, int h, SDL_Event & event) {
	}
};

void setInitialState() {
	setState(new CMInforeadState());
}

