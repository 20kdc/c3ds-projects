/*
 * ciesetup - The ultimate workarounds to fix an ancient game
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

/*
 * Implement audio via PortAudio.
 *
 * We have to tread carefully here.
 *
 * What's the problem? The problem is that libjack calls into libstdc++.
 * The game ships an incompatible libstdc++ replacement.
 * The maths on how that ends is not great (I found out the fun way).
 * So we have a needle that needs a bit of threading here.
 * libasound is going to screw us over if the user has a JACK ALSA plugin installed.
 * PortAudio just plain directly pulls in JACK.
 *
 * Given:
 * + all the other stuff the porters have mysteriously managed to get right...
 * + that a year 2000 build of SDL has ALSA support, implying they deliberately chose not to include ALSA
 * + the fact they're even shipping a custom libstdc++ in the first place
 *
 * I can't help but think this is some S-tier predictive skills at work on their part.
 *
 * ...However, I have a better idea. Meet LM_ID_NEWLM.
 * LM_ID_NEWLM allows cohabitance between the two worlds.
 * This is sort of like a Wine kinda deal where Wine hands off tasks to the "Linux side" of the process that the "Windows side" doesn't know about.
 * But instead of it being Linux and Windows sides, it's more "game side" (must be very careful about libs) vs. "new Linux side" (can do whatever it wants)
 */

#include <portaudio.h>

// Definitions

extern int puts(const char * str);
extern int putchar(int c);
extern int printf(const char * fmt, ...);
extern void * memset(void * s, int c, unsigned long n);

extern void * SDL_CreateMutex();
extern int SDL_mutexP(void *); // lock
extern void SDL_mutexV(void *); // unlock
extern void abort();

#define RTLD_LAZY 1
#define LM_ID_NEWLM -1
extern void * dlmopen(int, const char *, int);
extern void * dlsym(void *, const char *);

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
	void * userdata;
} audiospec_t;

// Stuff we always have

static void * mutex;
static void * paLibrary;
static int paInitialized;

// You might be wanting to ask what the point of a mutex is if we have to delay-create it this way.
// The answer is that the multi-threaded situations only come into play once SDL_OpenAudio is called.
// By that point we can guarantee it exists on all threads.
static void ensureMutex() {
	if (!mutex) {
		mutex = SDL_CreateMutex();
		if (!mutex) {
			printf("ciesetup: unable to create audio mutex, something has gone very wrong\n");
			abort();
		}
	}
}
static int ensureBasics() {
	if (!paLibrary)
		paLibrary = dlmopen(LM_ID_NEWLM, "libportaudio.so.2", RTLD_LAZY);
	if (!paLibrary) {
		printf("ciesetup: libportaudio.so.2 could not be loaded\n");
		return 1;
	}
	if (!paInitialized) {
		// alrighty, initialize Pa
		int (*pai)() = dlsym(paLibrary, "Pa_Initialize");
		// because it'd be a total mess to keep track of, we'll never properly de-initialize Pa
		if (pai()) {
			printf("ciesetup: Pa_Initialize failed\n");
			return 1;
		}
		paInitialized = 1;
	}
	return 0;
}

// Stream data
static void * paStream;
static audiospec_t sdlAudioSpec;

static int ourStreamCallback(const void * i, void * o, unsigned long frameCount, const PaStreamCallbackTimeInfo * ti, PaStreamCallbackFlags flags, void * blah) {
	memset(o, 0, frameCount * 4);
	SDL_mutexP(mutex);
	sdlAudioSpec.callback(sdlAudioSpec.userdata, o, (int) frameCount * 4);
	SDL_mutexV(mutex);
	return 0;
}

// if we call SDL_CloseAudio directly ld gets all weird about it
static void closeAudio() {
	if (paStream) {
		void (*pfn)(void *) = dlsym(paLibrary, "Pa_CloseStream");
		pfn(paStream);
	}
	paStream = (void *) 0;
}

int SDL_OpenAudio(audiospec_t * in, audiospec_t * out) {
	// ensure it isn't already open
	closeAudio();
	// and if it's not open, we know it's safe to muck with things!
	ensureMutex();
	sdlAudioSpec = *in;
	// copy this here so ourStreamCallback can get it
	// the format given is (always?) AUDIO_S16LSB
	printf("ciesetup: engine wants %ihz, %i channels, format %i\n", (int) sdlAudioSpec.sampleRate, (int) sdlAudioSpec.channels, (int) sdlAudioSpec.format);
	if (out) {
		// we get the opportunity to make adjustments here that SDL_mixer is forced to listen to
		// in practice, this means we're allowed to boost the sample rate
		// and make SURE that everything's within the parameters we use elsewhere
		out->channels = 2;
		out->sampleRate = 44100;
		out->format = 32784;
	}
	printf("ciesetup: will give %ihz, %i channels, format %i\n", (int) sdlAudioSpec.sampleRate, (int) sdlAudioSpec.channels, (int) sdlAudioSpec.format);
	if (ensureBasics()) {
		// whoops
		printf("ciesetup: basics were not available, faking audio\n");
	} else {
		// alright
		int (*pfn)(void **, int, int, int, double, int, void *, void *) = dlsym(paLibrary, "Pa_OpenDefaultStream");
		if (pfn(&paStream, 0, sdlAudioSpec.channels, paInt16, sdlAudioSpec.sampleRate, 512, ourStreamCallback, (void *) 0)) {
			printf("ciesetup: unable to open stream, faking audio\n");
			paStream = (void *) 0;
		} else {
			printf("ciesetup: opened audio stream, here we go\n");
			void (*pfn2)(void *) = dlsym(paLibrary, "Pa_StartStream");
			pfn2(paStream);
		}
	}
	// Note that if we fail, we want to lie to the engine so it doesn't do mean stuff to us
	if (out)
		*out = sdlAudioSpec;
	return 0;
}
void SDL_CloseAudio() {
	closeAudio();
}
void SDL_LockAudio() {
	ensureMutex();
	SDL_mutexP(mutex);
}
void SDL_UnlockAudio() {
	ensureMutex();
	SDL_mutexV(mutex);
}

