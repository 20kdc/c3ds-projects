# std vector
A vector appears to be roughly 12 bytes, and looks like this:


* +0: Pointer to start of vector
* +4: Pointer to end of vector (the immediately out-of-bounds pointer - this has to do with how C++ vector iteration works)
* +8: Scary intermediary variable - located somewhere within the vector



