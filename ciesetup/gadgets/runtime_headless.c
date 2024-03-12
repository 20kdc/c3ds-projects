/*
 * ciesetup - The ultimate workarounds to fix an ancient game
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

// null out crashing functions
void SDL_EraseCursor(void * blah) {
}

// scrap doesn't seem to be used and causes an application-wide failure for seemingly no reason, ditch it!
int init_scrap__Fv() asm("init_scrap__Fv");
int init_scrap__Fv() {
	// hmm
	return 0;
}

int lost_scrap__Fv() asm("lost_scrap__Fv");
int lost_scrap__Fv() {
	// hmm
	return 0;
}

void get_scrap__FiPiPPc(int t, int * p, char ** d) asm("get_scrap__FiPiPPc");
void get_scrap__FiPiPPc(int t, int * p, char ** d) {
	*p = 0;
}

void put_scrap__FiiPc() asm("put_scrap__FiiPc");
void put_scrap__FiiPc() {
}

// SDL shenanigans
int SDL_Init() {
	return 0;
}
