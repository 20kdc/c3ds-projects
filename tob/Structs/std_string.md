# std::string

Visual C++ 6 `std::string` -- look for `crt/xstring`.

These strings use a "quasi-copy-on-write" architecture.

An array of characters is allocated through `std::allocator`. A _long_ (too long) story _and_ several cross-verifications later, I learned that `operator new` and `operator delete` are `malloc` and `free` under different names.

In the array of characters, the first byte is an unsigned reference counter. The last is a null terminator for C compatibility. The length is the length of the string + 2 for this reason.

A reference count of 0 indicates the current owner is the only owner. A reference count of 255 indicates the string is 'frozen,' which also indicates the current owner is the only owner, but also locks it so that it stays that way.

Any operation that could lead to the buffer being modified freezes it. If the buffer has other owners, then the string is copied at that point. Any assignments from a frozen string, or from a string that would become frozen due to the reference count overflowing, copy immediately, creating a new unfrozen buffer.

Structure
---------

16 bytes:

* +0: `std::allocator`; Suffers from the empty class rule, should be safe to ignore.
* +4: char * data: When not NULL (empty string), this points to the _second_ element of the allocated array (the start of the data). Has the reference count at `data - 1` and a null terminator on the end of the text for C interchange.
* +8: size\_t length: String length, not including the null terminator.
* +12: size\_t allocationSize: The allocation is this long _+2._ There's one byte before the data (the reference count) and one after it (for a null terminator at the maximum length). A 'perfect' `std::string` would be `length == allocationSize`.