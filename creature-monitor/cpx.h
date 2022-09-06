/*
 * creature-monitor - watching norns from afar?
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

#pragma once

#include "main.h"

#include <stdint.h>

#define CAOS_PRINT_CM_HEADER "outs \"CMMagicHD\\n\"\n"
#define CAOS_PRINT_CM_FOOTER "outs \"CMMagicFT\"\n"

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

