/*
 * creature-monitor - watching norns from afar?
 * Written starting in 2022 by 20kdc
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

#include "main.h"
#include "cpx.h"

#include <stdio.h>

static CMBuffer * infoReadBuffer;
CMSlice cmChemicalNames[256];

class CMInforeadState : public CMState {
public:
	CPXRequestResult * result = NULL;
	CMPeriodic updateTimer = CMPeriodic(1000);

	~CMInforeadState() {
		delete result;
	}

	const char * stateName() { return "inforead"; }

	void frame(int w, int h) {
		if (result)
			writeText(0, 0, result->content.data, result->content.length);

		if (!updateTimer.shouldRun())
			return;

		if (result)
			delete result;

		result = cpxMakeRawRequest(
			"execute\n"
			// header
			CAOS_PRINT_CM_HEADER
			"outs \"\\n\"\n"
			// read out chemical names
			"setv va00 0\n"
			"loop\n"
			"outs read \"chemical_names\" va00\n"
			"outs \"\\n\"\n"
			"addv va00 1\n"
			"untl va00 eq 256\n"
			CAOS_PRINT_CM_FOOTER
		);

		// now is it valid?
		CMSlice clean;
		if (result->verifyMagic(clean)) {
			setSelectorState();
			infoReadBuffer = new CMBuffer(clean);
			CMSlice infoCursor = CMSlice(*infoReadBuffer);
			for (int i = 0; i < 256; i++)
				cmNextString(infoCursor, cmChemicalNames[i], '\n');
		}
	}
	void event(int w, int h, SDL_Event & event) {
	}
};

void setInitialState() {
	setState(new CMInforeadState());
}

