# We need to precompile ciesetup magic stuff.
# And we need to be careful about it because we want this to work on any distribution.
# We also need to ship ciesetup itself (which is shipped as a Makefile, sort of a DIY "fix the game" mechanism)

# Core Makefiles
rel: ciesetup/Makefile ciesetup/ds.mk ciesetup/c3.mk
# Manual
rel: ciesetup/README.md
# Python scripts
rel: ciesetup/gadgets/debz2.py ciesetup/gadgets/workarounds.py ciesetup/gadgets/workarounds.py
# Gadgets - workarounds.py
rel: ciesetup/gadgets/workarounds.deps
rel: ciesetup/gadgets/dummy.so ciesetup/gadgets/runtime.so
rel: ciesetup/gadgets/c3-inituser.cfg ciesetup/gadgets/c3-machine.cfg
# C3u2 patch marker
rel: ciesetup/gadgets/c3u2-patch.catalogue
# Acts as a placeholder file
rel: ciesetup/repo/.gitignore

# - Shared objects -

all: ciesetup/gadgets/dummy.so ciesetup/gadgets/runtime.so

ciesetup/gadgets/dummy.so: ciesetup/gadgets/dummy.c
	$(CC) -m32 -o $@ -shared -nostdlib $<

ciesetup/gadgets/runtime.so: ciesetup/gadgets/runtime_audio.c ciesetup/gadgets/runtime_dialogs.c
	$(CC) -m32 -o $@ -shared -nostdlib $^

