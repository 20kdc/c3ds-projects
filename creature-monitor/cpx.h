/*
 * creature-monitor - watching norns from afar?
 * Written starting in 2022 by 20kdc
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

#pragma once

#include "main.h"

#include <stdint.h>

class CMSlice {
public:
	char * data;
	size_t length;
	CMSlice() {}
	CMSlice(char * data, size_t length) : data(data), length(length) {}
	CMSlice slice(size_t pos) {
		return CMSlice(data + pos, length - pos);
	}
};

class CMBuffer : public CMObject, public CMSlice {
public:
	CMBuffer(const char * data, size_t len);
	CMBuffer(const CMSlice & slice) : CMBuffer(slice.data, slice.length) {}
	~CMBuffer();
};

class CPXRequestResult : public CMObject {
public:
	int resultCode;
	CMBuffer content;

	CPXRequestResult(const char * err);
	CPXRequestResult(int resultCode, int length);

	// checks for CMMagicHD and CMMagicFT text to verify integrity
	bool verifyMagic(CMSlice & cleanSlice);
};

CPXRequestResult * cpxMakeRawRequest(const char * request);

// Advances a slice's beginning to the start of the next line/whatever.
// Returns false if there isn't one.
// Note that if the split char is found at the very end of the slice, false is returned (there's no more actual data)
bool cpxNextString(CMSlice & slice, CMSlice & content, char split);

