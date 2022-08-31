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
	int id; // always equal to neuron index
	float values[8];
} neuron_t;

typedef struct {
	int id;
	int srcNeuron;
	int dstNeuron;
	float values[8];
} dendrite_t;

typedef struct {
	int id;
	int decisionIndex;
	char name[4];
	int d;
	int x, y, w, h;
	int blah[4];
	ruleset_t init;
	ruleset_t update;
	// neurons follow
	neuron_t neurons[];
	// then lobefoot_t
} lobehdr_t;

typedef struct {
	int a;
	int b;
	int srcLobe;
	int srcMin;
	int srcUnk;
	char srcUnk2;
	int dstLobe;
	int dstMin;
	int dstUnk;
	char dstUnk2;
	bool flagA;
	bool flagB;
	ruleset_t init;
	ruleset_t update;
	int dendriteCount;
	// dendrites follow
	// then lobefoot_t
} tracthdr_t;

typedef struct {
	char footer[10];
} lobefoot_t;
#pragma pack(pop)

#define BRN_GRIDW 16
#define BRN_GRIDH 12

static SDL_Rect getCellRegion(int ofsX, int ofsY, int x, int y, int w, int h) {
	return {ofsX + (x * BRN_GRIDW), ofsY + (y * BRN_GRIDH), w * BRN_GRIDW, h * BRN_GRIDH};
}

static SDL_Rect getCellRegion(int ofsX, int ofsY, lobehdr_t * lobe, int i) {
	return getCellRegion(ofsX, ofsY, lobe->x + (i % lobe->w), lobe->y + (i / lobe->w), 1, 1);
}

static int decideColour(float f) {
	float datums[] = {
		f * -16,
		fabs(f),
		f * 16,
	};
	int col = 0;
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
	return col;
}

class CMBrainState : public CMState {
public:
	CPXRequestResult * result = NULL;
	CMPeriodic updateTimer = CMPeriodic(100);
	CMBuffer moniker;
	char * stateNameDetail;
	int lobes, tracts;
	bool dendritesView = false;

	CMBrainState(const CMSlice & moniker, int l, int t) : moniker(moniker), lobes(l), tracts(t) {
		stateNameDetail = (CMSlice("brain:") + moniker + CMSlice(" L") + cmItoB(l) + CMSlice(" T") + cmItoB(t)).dupCStr();
	}

	~CMBrainState() {
		free(stateNameDetail);
		delete result;
	}

	const char * stateName() { return stateNameDetail; }

	void onDraw(const SDL_Point & mouseAt) {
		if (result) {
			if (!result->resultCode) {
				CMSlice cursor = result->content;
				// cmDumpSliceToFile(cursor, "lobedump.bin");
				int ofsX = 32;
				int ofsY = 32;
				lobehdr_t * lobeHeaders[lobes];
				for (int i = 0; i < lobes; i++) {
					lobehdr_t * lobehdr = (lobehdr_t *) cursor.grab(sizeof(lobehdr_t));
					// store for later access
					lobeHeaders[i] = lobehdr;
					// printf("%c%c%c%c %i %i %i %i\n", lobehdr->name[0], lobehdr->name[1], lobehdr->name[2], lobehdr->name[3], lobehdr->x, lobehdr->y, lobehdr->w, lobehdr->h);

					int neuronCount = lobehdr->w * lobehdr->h;
					// printf(" total neuron blob size %i\n", (int) (neuronCount * sizeof(neuron_t)));
					neuron_t * neurons = (neuron_t *) cursor.grab(neuronCount * sizeof(neuron_t));

					// footer
					cursor.grab(sizeof(lobefoot_t));

					// now draw
					SDL_Rect lobe = getCellRegion(ofsX, ofsY, lobehdr->x, lobehdr->y, lobehdr->w, lobehdr->h);
					fillRect(lobe, 0xFFFFFFFF);
					fillRect(rcMargin(lobe, 2), 0xFF000000);

					writeText(ptOfs(rcUL(lobe), {0, -16}), lobehdr->name, 4);
					// driv ends at 0x55A / 1370
					// printf(" end at %i\n", (int) (cursor.data - result->content.data));

					for (int j = 0; j < neuronCount; j++) {
						SDL_Rect nbox = getCellRegion(ofsX, ofsY, lobehdr, j);
						SDL_Rect nboxInner = rcMargin(nbox, 4);
						if (lobehdr->decisionIndex == j)
							fillRect(nbox, 0xFF808080);
						fillRect(nboxInner, decideColour(neurons[j].values[0]));
					}
				}

				for (int i = 0; i < tracts; i++) {
					tracthdr_t * tracthdr = (tracthdr_t *) cursor.grab(sizeof(tracthdr_t));

					dendrite_t * dendrites = (dendrite_t *) cursor.grab(tracthdr->dendriteCount * sizeof(dendrite_t));

					// need to find lobe
					lobehdr_t * srcLobeHdr = NULL;
					lobehdr_t * dstLobeHdr = NULL;
					for (int j = 0; j < lobes; j++) {
						if (lobeHeaders[j]->id == tracthdr->srcLobe)
							srcLobeHdr = lobeHeaders[j];
						if (lobeHeaders[j]->id == tracthdr->dstLobe)
							dstLobeHdr = lobeHeaders[j];
					}

					if (srcLobeHdr && dstLobeHdr) {
						int srcLobeHdrNC = srcLobeHdr->w * srcLobeHdr->h;
						int dstLobeHdrNC = dstLobeHdr->w * dstLobeHdr->h;
						// have both lobes!
						for (int j = 0; j < tracthdr->dendriteCount; j++) {
							int sni = dendrites[j].srcNeuron;
							if (sni < 0 || sni >= srcLobeHdrNC) {
								continue;
							}
							if (dendritesView) {
								SDL_Rect srcNeuron = getCellRegion(ofsX, ofsY, srcLobeHdr, sni);
								SDL_Rect dstNeuron = getCellRegion(ofsX, ofsY, dstLobeHdr, dendrites[j].dstNeuron);
								float f = dendrites[j].values[0] * srcLobeHdr->neurons[sni].values[0];
								drawLine(rcCentre(srcNeuron), rcCentre(dstNeuron), decideColour(f));
							}
						}
					}

					// footer
					cursor.grab(sizeof(lobefoot_t));
				}
			} else {
				writeText({0, 0}, result->content.data, result->content.length);
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
	void onKeyDown(int sym) {
		if (sym == SDLK_BACKSPACE) {
			setSelectorState();
		} else if (sym == SDLK_d) {
			dendritesView = !dendritesView;
		} else if (sym == SDLK_RETURN) {
			setChemState(moniker);
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

	void onDraw(const SDL_Point & mouseAt) {
		if (result)
			writeText({0, 0}, result->content.data, result->content.length);

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
	void onKeyDown(int sym) {
		if (sym == SDLK_BACKSPACE) {
			setSelectorState();
		} else if (sym == SDLK_RETURN) {
			setChemState(moniker);
		}
	}
};


void setBrainState(const CMSlice & moniker) {
	setState(new CMBrainSetupState(moniker));
}

