/*
 * creature-monitor - watching norns from afar?
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

#include "main.h"
#include "cpx.h"

#include <stdio.h>

class CMChemState : public CMState {
public:
	CPXRequestResult * result = NULL;
	CMPeriodic updateTimer = CMPeriodic(100);
	CMBuffer moniker;
	char * stateNameDetail;

	CMChemState(const CMSlice & moniker) : moniker(moniker) {
		stateNameDetail = (CMSlice("chem:") + moniker).dupCStr();
	}

	~CMChemState() {
		free(stateNameDetail);
		delete result;
	}

	const char * stateName() { return stateNameDetail; }

	void onDraw(const SDL_Point & mouseAt) {
		if (result) {
			CMSlice clean;
			if (result->verifyMagic(clean)) {
				for (int i = 0; i < 256; i++) {
					int x = (i & 7) * 192;
					int y = (i >> 3) * 32;
					writeText({x, y}, cmChemicalNames[i]);
					CMSlice line;
					cmNextString(clean, line, '\n');
					char * val = line.dupCStr();
					double n = atof(val);
					free(val);
					writeText({x, y + 16}, line);
					SDL_Rect fullBar = {x + 66, y + 16, 124, 16};
					fillRect(fullBar, 0xFFFFFFFF);
					fullBar.x += 1; fullBar.y += 1; fullBar.w -= 2; fullBar.h -= 2;
					fillRect(fullBar, 0xFF000000);
					fullBar.x += 1; fullBar.y += 1; fullBar.w -= 2; fullBar.h -= 2;
					fullBar.w = (int) (fullBar.w * n);
					fillRect(fullBar, 0xFFFFFFFF);
				}
			} else {
				writeText({0, 0}, result->content.data, result->content.length);
			}
		}

		if (!updateTimer.shouldRun())
			return;

		if (result)
			delete result;

		char * request = (CMSlice(
			"execute\n"
			"targ mtoc \""
		) + moniker + CMSlice(
			"\"\n"
			// header
			CAOS_PRINT_CM_HEADER
			// read out chemical values
			"setv va00 0\n"
			"loop\n"
			"outv chem va00\n"
			"outs \"\\n\"\n"
			"addv va00 1\n"
			"untl va00 eq 256\n"
			CAOS_PRINT_CM_FOOTER
		)).dupCStr();
		result = cpxMakeRawRequest(request);
		free(request);
	}
	void onKeyDown(int sym) {
		if (sym == SDLK_BACKSPACE) {
			setSelectorState();
		} else if (sym == SDLK_RETURN) {
			setBrainState(moniker);
		}
	}
};

void setChemState(const CMSlice & moniker) {
	setState(new CMChemState(moniker));
}

