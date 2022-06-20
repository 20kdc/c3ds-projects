/*
 * ciesetup - The ultimate workarounds to fix an ancient game
 * Written starting in 2022 by 20kdc
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

// Definitions

extern int puts(const char * str);
extern int putchar(int c);
extern int printf(const char * fmt, ...);
extern int scanf(const char * fmt, ...);

// SDL Audio (Except It's Not Really)

typedef struct {
	int sampleRate;
	unsigned short format;
	unsigned char channels;
	unsigned char calcSilence;
	unsigned short samples;
	char padding[2]; // meh!
	unsigned int outSize;
	void (*callback)(void *, unsigned char *, int);
	void * useradata;
} audiospec_t;

// Not done yet!

/*
int SDL_OpenAudio(audiospec_t * in, audiospec_t * out) {
	*out = *in;
	return 0;
}
void SDL_CloseAudio() {
}
void SDL_LockAudio() {
}
void SDL_UnlockAudio() {
}
*/

