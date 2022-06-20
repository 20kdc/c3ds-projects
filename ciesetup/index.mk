# We need to precompile ciesetup magic stuff.
# And we need to be careful about it because we want this to work on any distribution.
# We also need to ship ciesetup itself (which is shipped as a Makefile, sort of a DIY "fix the game" mechanism)

all: ciesetup/gadgets/dummy.so ciesetup/gadgets/runtime.so

rel: ciesetup/Makefile ciesetup/README.md
rel: ciesetup/gadgets/debz2.py ciesetup/gadgets/workarounds.py ciesetup/gadgets/workarounds.deps
rel: ciesetup/gadgets/dummy.so ciesetup/gadgets/runtime.so
rel: ciesetup/repo/.gitignore

ciesetup/gadgets/dummy.so: ciesetup/gadgets/dummy.c
	gcc -m32 -o $@ -shared -nostdlib $<

ciesetup/gadgets/runtime.so: ciesetup/gadgets/runtime_audio.c ciesetup/gadgets/runtime_dialogs.c
	gcc -m32 -o $@ -shared -nostdlib $^

