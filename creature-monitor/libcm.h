/*
 * creature-monitor - watching norns from afar?
 * Written starting in 2022 by 20kdc
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

#pragma once

class CMObject {
public:
	CMObject() {}
	CMObject(const CMObject &) = delete;
	virtual ~CMObject() {}
	void queueDelete();
	static void performQueuedDeletions();
private:
	CMObject * _nextInDeleteQueue;
	static CMObject * _deleteQueue;
};

extern void cmPerformQueuedDeletions();

class CMSlice {
public:
	char * data;
	size_t length;
	CMSlice() {}
	CMSlice(char * data, size_t length) : data(data), length(length) {}
	CMSlice slice(size_t pos) {
		return CMSlice(data + pos, length - pos);
	}
	CMSlice slice(size_t pos, size_t len) {
		return CMSlice(data + pos, length - pos);
	}
};

class CMBuffer : public CMObject, public CMSlice {
public:
	CMBuffer(const char * data, size_t len);
	CMBuffer(const CMSlice & slice) : CMBuffer(slice.data, slice.length) {}
	~CMBuffer();
};

// Advances a slice's beginning to the start of the next line/whatever.
// Returns false if there isn't one.
// Note that if the split char is found at the very end of the slice, false is returned (there's no more actual data)
bool cmNextString(CMSlice & slice, CMSlice & content, char split);

