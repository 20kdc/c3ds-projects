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

// Fake SDL's video subsystem
int SDL_Init() {
	return 0;
}

typedef struct {
	void * pal;
	char bipp, bypp, rl, gl, bl, al, rs, gs, bs, as;
	int rm, gm, bm, am;
	int ck;
	char a;
} sdl_px_t;

typedef struct {
	int flags;
	int vmem;
	const sdl_px_t * vfmt;
	int w;
	int h;
} sdl_vi_t;

typedef struct {
	short x, y, w, h;
} sdl_r_t;

static const sdl_px_t the_pixel_format = {
	.bipp = 16,
	.bypp = 2,
	.rs = 11,
	.rm = 0xF800,
	.gs = 5,
	.gm = 0x07E0,
	.bs = 0,
	.bm = 0x001F
};

static const sdl_vi_t the_video_format = {
	.flags = 0,
	.vmem = 0,
	.vfmt = &the_pixel_format
};

const sdl_vi_t * SDL_GetVideoInfo() {
	return &the_video_format;
}

void * SDL_ListModes() {
	return (void *) -1;
}

extern void * SDL_CreateRGBSurface(int f, int w, int h, int d, int r, int g, int b, int a);

void * SDL_SetVideoMode(int w, int h, int bpp, int flags) {
	return SDL_CreateRGBSurface(0, w, h, 16, 0xF800, 0x07E0, 0x001F, 0);
}

void SDL_UpdateRect() {
}
