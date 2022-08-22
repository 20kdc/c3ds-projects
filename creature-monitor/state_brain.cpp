/*
 * creature-monitor - watching norns from afar?
 * Written starting in 2022 by 20kdc
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

#include "main.h"
#include "cpx.h"

#include <stdio.h>
#include <math.h>

#pragma pack(push,1)
typedef struct {
	int a, b, c;
	float f;
} rule_t;

typedef struct {
	rule_t rules[16];
} ruleset_t;

typedef struct {
	float input;
	int b;
	float values[8];
} neuron_t;

typedef struct {
	int a;
	int decisionIndex;
	char name[4];
	int d;
	int x, y, w, h;
	int blah[4];
	ruleset_t init;
	ruleset_t update;
	// neurons follow
	// then another random thing
} lobehdr_t;

typedef struct {
	char footer[10];
} lobefoot_t;
#pragma pack(pop)

#define BRN_GRIDW 16
#define BRN_GRIDH 16

static SDL_Rect getCellRegion(int ofsX, int ofsY, int x, int y, int w, int h) {
	return {ofsX + (x * BRN_GRIDW), ofsY + (y * BRN_GRIDH), w * BRN_GRIDW, h * BRN_GRIDH};
}

class CMBrainState : public CMState {
public:
	CPXRequestResult * result = NULL;
	CMPeriodic updateTimer = CMPeriodic(100);
	CMBuffer moniker;
	char * stateNameDetail;
	int lobes, tracts;

	CMBrainState(const CMSlice & moniker, int l, int t) : moniker(moniker), lobes(l), tracts(t) {
		stateNameDetail = (CMSlice("brain:") + moniker + CMSlice(" L") + cmItoB(l) + CMSlice(" T") + cmItoB(t)).dupCStr();
	}

	~CMBrainState() {
		free(stateNameDetail);
		delete result;
	}

	const char * stateName() { return stateNameDetail; }

	void frame(int w, int h) {
		if (result) {
			if (!result->resultCode) {
				CMSlice cursor = result->content;
				// cmDumpSliceToFile(cursor, "lobedump.bin");
				int ofsX = 32;
				int ofsY = 32;
				for (int i = 0; i < lobes; i++) {
					lobehdr_t * lobehdr = (lobehdr_t *) cursor.data;
					// printf("%c%c%c%c %i %i %i %i\n", lobehdr->name[0], lobehdr->name[1], lobehdr->name[2], lobehdr->name[3], lobehdr->x, lobehdr->y, lobehdr->w, lobehdr->h);
					cursor = cursor.slice(sizeof(lobehdr_t));

					neuron_t * neurons = (neuron_t *) cursor.data;
					int neuronCount = lobehdr->w * lobehdr->h;
					// printf(" total neuron blob size %i\n", (int) (neuronCount * sizeof(neuron_t)));
					cursor = cursor.slice(neuronCount * sizeof(neuron_t));

					// footer
					cursor = cursor.slice(sizeof(lobefoot_t));

					// now draw
					SDL_Rect lobe = getCellRegion(ofsX, ofsY, lobehdr->x, lobehdr->y, lobehdr->w, lobehdr->h);
					fillRect(lobe, 0xFFFFFFFF);
					fillRect(marginRect(lobe, 2), 0xFF000000);

					writeText(lobe.x, lobe.y - 16, lobehdr->name, 4);
					// driv ends at 0x55A / 1370
					// printf(" end at %i\n", (int) (cursor.data - result->content.data));

					for (int j = 0; j < neuronCount; j++) {
						int nX = j % lobehdr->w;
						int nY = j / lobehdr->w;
						SDL_Rect nbox = getCellRegion(ofsX, ofsY, lobehdr->x + nX, lobehdr->y + nY, 1, 1);
						SDL_Rect nboxInner = marginRect(nbox, 4);
						int col = 0;
						float datums[] = {
							neurons[j].values[0] * -16,
							fabs(neurons[j].values[0]),
							neurons[j].values[0] * 16,
						};
						for (int k = 0; k < 3; k++) {
							int subCol = (int) (datums[k] * 255);
							if (subCol < 0)
								subCol = 0;
							if (subCol > 255)
								subCol = 255;
							col <<= 8;
							col |= subCol;
						}
						col |= 0xFF000000;
						if (lobehdr->decisionIndex == j)
							fillRect(nbox, 0xFF808080);
						fillRect(nboxInner, col);
					}
				}
			} else {
				writeText(0, 0, result->content.data, result->content.length);
			}
		}

		if (!updateTimer.shouldRun())
			return;

		if (result)
			delete result;

		CMBuffer request = CMSlice("execute\ntarg mtoc \"") + moniker + CMSlice("\"\n");
		for (int i = 0; i < lobes; i++) {
			request = request + CMSlice("brn: dmpl ");
			request = request + cmItoB(i);
			request = request + CMSlice("\n");
		}
		for (int i = 0; i < tracts; i++) {
			request = request + CMSlice("brn: dmpt ");
			request = request + cmItoB(i);
			request = request + CMSlice("\n");
		}
		char * requestC = request.dupCStr();
		result = cpxMakeRawRequest(requestC);
		free(requestC);
	}
	void event(int w, int h, SDL_Event & event) {
		if (event.type == SDL_KEYDOWN) {
			if (event.key.keysym.sym == SDLK_BACKSPACE) {
				setSelectorState();
			} else if (event.key.keysym.sym == SDLK_RETURN) {
				setChemState(moniker);
			}
		}
	}
};

class CMBrainSetupState : public CMState {
public:
	CPXRequestResult * result = NULL;
	CMPeriodic updateTimer = CMPeriodic(100);
	CMBuffer moniker;
	char * stateNameDetail;

	CMBrainSetupState(const CMSlice & moniker) : moniker(moniker) {
		stateNameDetail = (CMSlice("brain-setup:") + moniker).dupCStr();
	}

	~CMBrainSetupState() {
		free(stateNameDetail);
		delete result;
	}

	const char * stateName() { return stateNameDetail; }

	void frame(int w, int h) {
		if (result)
			writeText(0, 0, result->content.data, result->content.length);

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
			"brn: dmpb\n"
			CAOS_PRINT_CM_FOOTER
		)).dupCStr();
		result = cpxMakeRawRequest(request);
		free(request);

		CMSlice clean;
		if (result->verifyMagic(clean)) {
			CMSlice line;

			cmNextString(clean, line, 0);
			int lobes = atoi(line.data);

			cmNextString(clean, line, 0);
			int tracts = atoi(line.data);

			setState(new CMBrainState(moniker, lobes, tracts));
		}
	}
	void event(int w, int h, SDL_Event & event) {
		if (event.type == SDL_KEYDOWN) {
			if (event.key.keysym.sym == SDLK_BACKSPACE) {
				setSelectorState();
			} else if (event.key.keysym.sym == SDLK_RETURN) {
				setChemState(moniker);
			}
		}
	}
};


void setBrainState(const CMSlice & moniker) {
	setState(new CMBrainSetupState(moniker));
}

