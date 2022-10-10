# RAL Manual

RAL is a statically typed language that compiles to CAOS. *(While "AL" can be reasonably assumed to be "Agent Language", the meaning of the R may yet come under heated debate in parlours of tea-drinking across the countryside - if they are sun-lit or not is a matter of timing, of course. Complicating this matter is the compiler's package name of `rals`, which does little to improve the situation. While theories have not yet reached the heights of "Real Agents Loath Shee", that time is probably coming, eventually.)*

The purpose of RAL is to allow for working on complex modding projects without having to worry about the typical problems that come with that in CAOS, such as keeping track of variable names by number.

RAL is split into a compiler (itself written in Java) and a standard library (*doesn't yet exist*) covering Creatures 3 and Docking Station.

As such, this manual is intended to be written in four distinct sections:

+ The *Tooling Reference* covers how to use the RAL compiler to compile RAL source code into CAOS, alongside other tasks.

+ The *Language Reference* covers RAL as a language, and constructs universal to the compiler.

+ The *Language Technical Details* cover scary technical bits of RAL - this may be important to look at if you're encountering something weird.

+ The *Library Reference* covers RAL's standard library.

+ The *Guides* cover instructional material on RAL's usage.
