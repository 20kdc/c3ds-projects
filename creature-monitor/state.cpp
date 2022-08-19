/*
 * creature-monitor - watching norns from afar?
 * Written starting in 2022 by 20kdc
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

#include "main.h"
#include "cpx.h"

#include <stdio.h>

class CMTestState : public CMState {
public:
	Uint32 nextCheck = 0;
	CPXRequestResult * result = NULL;

	void frame(int w, int h) {
		if (result) {
			CMSlice slice;
			if (result->verifyMagic(slice)) {
				if (slice.length > 0) {
					writeText(8, 8, "Select Target:");
					int y = 32;
					CMSlice line;
					while (cmNextString(slice, line, 10)) {
						writeText(w - 256, y, line.data, line.length);
						cmNextString(slice, line, 10);
						writeText(0, y, line.data, line.length);
						y += 16;
					}
				}
			} else {
				writeText(0, 0, result->content.data, result->content.length);
			}
		}

		Uint32 currentTicks = SDL_GetTicks();
		if (currentTicks > nextCheck) {
			nextCheck = currentTicks + 1000;

			if (result)
				delete result;

			// do CPX request to locate them Norns
			result = cpxMakeRawRequest(
				"execute\n"
				"outs \"CMMagicHD\\n\"\n"
				// give the selected creature special attention
				"targ norn\n"
				"doif targ ne null\n"
					"outs gtos 0\n"
					"outs \"\\n\"\n"
					"outs hist name gtos 0\n"
					"outs \" (selected)\\n\"\n"
				"endi\n"
				// go over the others
				"enum 4 0 0\n"
					"outs gtos 0\n"
					"outs \"\\n\"\n"
					"outs hist name gtos 0\n"
					"outs \"\\n\"\n"
				"next\n"
				"outs \"CMMagicFT\"\n"
			);
		}
	}
	void event(int w, int h, SDL_Event & event) {
	}
};

void setInitialState() {
	gCurrentState = new CMTestState();
}

