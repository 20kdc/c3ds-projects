/*
 * creature-monitor - watching norns from afar?
 * Written starting in 2022 by 20kdc
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

#include <stddef.h>
#include <stdlib.h>
#include <string.h>

#include "libcm.h"

CMObject * CMObject::_deleteQueue;

void * operator new(size_t sz) {
	return malloc(sz);
}

void operator delete(void * obj, size_t) {
	free(obj);
}

CMBuffer::CMBuffer(const char * d, size_t l) {
	if (!l) {
		d = NULL;
		data = NULL;
		length = 0;
	} else {
		data = (char *) malloc(l);
		length = l;
	}
	if (d)
		memcpy(data, d, l);
}

CMBuffer::~CMBuffer() {
	free(data);
}

void CMObject::queueDelete() {
	_nextInDeleteQueue = _deleteQueue;
	_deleteQueue = this;
}

void CMObject::performQueuedDeletions() {
	while (_deleteQueue) {
		CMObject * qd = _deleteQueue;
		_deleteQueue = qd->_nextInDeleteQueue;
		delete qd;
	}
}

bool cmNextString(CMSlice & slice, CMSlice & content, char split) {
	for (int i = 0; i < slice.length; i++) {
		if (slice.data[i] == split) {
			// advance!
			content = CMSlice(slice.data, i);
			slice = slice.slice(i + 1);
			return true;
		}
	}
	// ran off end without hitting a terminator
	return false;
}

CMBuffer cmAppend(CMSlice & a, CMSlice & b) {
	CMBuffer buf(NULL, a.length + b.length);
	memcpy(buf.data, a.data, a.length);
	memcpy(buf.data + a.length, b.data, b.length);
	return buf;
}

