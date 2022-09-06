/*
 * ragnarok - a device to improve Wine compatibility with Docking Station
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

#include <stdlib.h>
#include <stdint.h>
#include <stdio.h>
#include <string.h>
#include <windows.h>

#define DDRHK_SEARCHBUF_SIZE 28
int coreProcessInner(FILE * file) {
	const char * searchBuffer1 = "DirectDrawCreate\0\0DDRAW.dll\0";
	// using a separate name allows DDRHK.dll to easily import DDRAW.dll
	const char * writeBuffer = "DirectDrawHooked\0\0DDRHK.dll\0";
	char searchBuffer2[DDRHK_SEARCHBUF_SIZE];
	for (int i = 0; i < DDRHK_SEARCHBUF_SIZE; i++)
		searchBuffer2[i] = fgetc(file);
	while (1) {
		// compare
		if (!memcmp(searchBuffer1, searchBuffer2, DDRHK_SEARCHBUF_SIZE))
			break;
		// inject new char
		int chr = fgetc(file);
		if (chr == -1) {
			MessageBoxA(NULL, "Unable to locate DirectDrawCreate import (was already patched, not a static import, or wrong search string)", "DDRHK", MB_OK);
			return 1;
		}
		for (int i = 0; i < DDRHK_SEARCHBUF_SIZE - 1; i++)
			searchBuffer2[i] = searchBuffer2[i + 1];
		searchBuffer2[DDRHK_SEARCHBUF_SIZE - 1] = chr;
	}
	// the current file position is at the end of the search string, we need to go back
	fseek(file, -DDRHK_SEARCHBUF_SIZE, SEEK_CUR);
	for (int i = 0; i < DDRHK_SEARCHBUF_SIZE; i++)
		fputc(writeBuffer[i], file);
	MessageBoxA(NULL, "Successfully patched.", "DDRHK", MB_OK);
	return 0;
}

int coreProcessOuter(FILE * file) {
	if (!file) {
		MessageBoxA(NULL, "Unable to open file.", "DDRHK", MB_OK);
		return 1;
	}
	int res = coreProcessInner(file);
	fclose(file);
	return res;
}

int main(int argc, char ** argv) {
	if (argc == 2)
		return coreProcessOuter(fopen(argv[1], "r+b"));

	char fileName[4096];
	fileName[0] = 0;
	OPENFILENAMEA ofa = {};
	ofa.lStructSize = sizeof(ofa);
	ofa.lpstrFilter = "Executable\0*.exe\0All Files\0*.*\0\0";
	ofa.lpstrTitle = "Select executable to apply DDRHK hook";
	ofa.lpstrFile = fileName;
	ofa.nMaxFile = 4096;
	if (GetOpenFileNameA(&ofa))
		return coreProcessOuter(fopen(fileName, "r+b"));
	return 1;
}

