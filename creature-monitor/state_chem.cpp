/*
 * creature-monitor - watching norns from afar?
 * Written starting in 2022 by 20kdc
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

#include "main.h"
#include "cpx.h"

#include <stdio.h>

class CMChemState : public CMState {
public:
	CPXRequestResult * result = NULL;
	CMPeriodic updateTimer = CMPeriodic(1000);
	CMBuffer moniker;

	CMChemState(const CMSlice & moniker) : moniker(moniker) {
		
	}

	~CMChemState() {
		delete result;
	}

	const char * stateName() { return "chem"; }

	void frame(int w, int h) {
		if (result) {
			CMSlice clean;
			if (result->verifyMagic(clean)) {
				for (int i = 0; i < 256; i++) {
					int x = (i & 7) * 192;
					int y = (i >> 3) * 32;
					writeText(x, y, cmChemicalNames[i]);
					CMSlice line;
					cmNextString(clean, line, '\n');
					writeText(x, y + 16, line);
				}
			}
		}

		if (!updateTimer.shouldRun())
			return;

		if (result)
			delete result;

		result = cpxMakeRawRequest(
			"execute\n"
			// header
			CAOS_PRINT_CM_HEADER
			// read out chemical values
			"setv va00 0\n"
			"targ norn\n"
			"loop\n"
			"outv chem va00\n"
			"outs \"\\n\"\n"
			"addv va00 1\n"
			"untl va00 eq 256\n"
			CAOS_PRINT_CM_FOOTER
		);
	}
	void event(int w, int h, SDL_Event & event) {
	}
};

void setChemState(const CMSlice & moniker) {
	setState(new CMChemState(moniker));
}

