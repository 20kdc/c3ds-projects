/*
 * creature-monitor - watching norns from afar?
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

#include "main.h"
#include "cpx.h"

#include <stdio.h>

#define LINE_START_Y 32
#define LINE_HEIGHT 16

class CMSelectorState : public CMState {
public:
	CPXRequestResult * result = NULL;
	int selectedLine = -1;

	CMPeriodic updateTimer = CMPeriodic(1000);

	~CMSelectorState() {
		delete result;
	}

	const char * stateName() { return "selector"; }

	void updateSelectedLine(const SDL_Point & mouseAt) {
		selectedLine = (mouseAt.y - LINE_START_Y) / LINE_HEIGHT;
	}

	void onDraw(const SDL_Point & mouseAt) {
		updateSelectedLine(mouseAt);
		if (result) {
			SDL_Rect b = bounds();
			CMSlice slice;
			if (result->verifyMagic(slice)) {
				if (slice.length > 0) {
					writeText({8, 8}, "Select Target:");
					int y = LINE_START_Y;
					CMSlice line;
					int lineId = 0;
					while (cmNextString(slice, line, 10)) {
						if (lineId == selectedLine)
							fillRect({0, y, b.w, LINE_HEIGHT}, 0xFF000080);
						writeText({b.w - 256, y}, line.data, line.length);
						cmNextString(slice, line, 10);
						writeText({0, y}, line.data, line.length);
						y += LINE_HEIGHT;
						lineId++;
					}
				}
			} else {
				writeText({0, 0}, result->content.data, result->content.length);
			}
		}

		if (updateTimer.shouldRun()) {
			if (result)
				delete result;

			// do CPX request to locate them Norns
			result = cpxMakeRawRequest(
				"execute\n"
				CAOS_PRINT_CM_HEADER
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
				CAOS_PRINT_CM_FOOTER
			);
		}
	}
	bool onClick(const SDL_Point & mouseAt) {
		updateSelectedLine(mouseAt);
		if (result) {
			CMSlice slice;
			if (result->verifyMagic(slice)) {
				if (slice.length > 0) {
					CMSlice line;
					int lineId = 0;
					while (cmNextString(slice, line, 10)) {
						if (lineId == selectedLine) {
							setChemState(line);
							return true;
						}
						cmNextString(slice, line, 10);
						lineId++;
					}
				}
			}
		}
		return false;
	}
};

void setSelectorState() {
	setState(new CMSelectorState());
}

