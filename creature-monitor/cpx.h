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
};

class CMBuffer : public CMObject, public CMSlice {
public:
	CMBuffer(const char * data, size_t len);
	~CMBuffer();
};

class CPXRequestResult : public CMObject {
public:
	int resultCode;
	CMBuffer content;

	CPXRequestResult(const char * err);
	CPXRequestResult(int resultCode, int length);
};

CPXRequestResult * cpxMakeRawRequest(const char * request);

