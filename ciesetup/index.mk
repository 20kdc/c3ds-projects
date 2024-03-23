# We need to precompile ciesetup magic stuff.
# And we need to be careful about it because we want this to work on any distribution.
# We also need to ship ciesetup itself (which is shipped as a Makefile, sort of a DIY "fix the game" mechanism)

.PHONY: ciesetup
ciesetup: ciesetup/gadgets/dummy.so ciesetup/gadgets/runtime.so ciesetup/gadgets/runtime_headless.so

# Core Makefiles
rel: ciesetup/Makefile ciesetup/ds.mk ciesetup/c3.mk
# Manual
rel: ciesetup/README.md
# Python scripts
rel: ciesetup/gadgets/debz2.py
rel: ciesetup/gadgets/preplib.py
rel: ciesetup/gadgets/prep_dockingstation.py
rel: ciesetup/gadgets/prep_creatures3.py
rel: ciesetup/gadgets/prep_engine.py
# C3u2 patch marker
rel: ciesetup/gadgets/c3u2-patch.catalogue
# Regular scripts
rel: ciesetup/gadgets/run-game
# Acts as a placeholder file
rel: ciesetup/repo/.gitignore

# - Docker -

rel: ciesetup/docker/build
rel: ciesetup/docker/build-and-test
rel: ciesetup/docker/c2e.Dockerfile
rel: ciesetup/docker/c3ds.Dockerfile
rel: ciesetup/docker/ds.Dockerfile

# - Shared objects -

rel: ciesetup/gadgets/dummy.so ciesetup/gadgets/runtime.so ciesetup/gadgets/runtime_headless.so
all: ciesetup/gadgets/dummy.so ciesetup/gadgets/runtime.so ciesetup/gadgets/runtime_headless.so

ciesetup/gadgets/dummy.so: ciesetup/gadgets/dummy.c
	$(CC) -m32 -fno-stack-protector -o $@ -shared -nostdlib $<

ciesetup/gadgets/runtime.so: ciesetup/gadgets/runtime_audio.c ciesetup/gadgets/runtime_dialogs.c
	$(CC) -m32 -fno-stack-protector -o $@ -shared -nostdlib $^

ciesetup/gadgets/runtime_headless.so: ciesetup/gadgets/runtime_audio.c ciesetup/gadgets/runtime_dialogs.c ciesetup/gadgets/runtime_headless.c
	$(CC) -m32 -fno-stack-protector -DHEADLESS -o $@ -shared -nostdlib $^
