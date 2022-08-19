# Attempt to auto-detect OS.
# Note that MSYS2 should be considered a "Cygwin" host.
# Such a host doesn't have SDL2 directly, but it can cross-compile.

ifeq ($(shell uname),Linux)
HOST_LINUX ?= 1
HOST_SDL2 ?= 1
endif

# Assume we don't have SDL2 but leave it open for an override
# MSYS2 uses something like MSYS_NT-6.1-7601 in these cases, so comparison is hard
HOST_LINUX ?= 0
HOST_SDL2 ?= 0

# NOTE:
# https://www.libsdl.org/download-2.0.php
# https://github.com/libsdl-org/SDL_net/releases
W32_SDL2 ?= 1

# Windows
W32_CC ?= i686-w64-mingw32-gcc
W32_STRIP ?= i686-w64-mingw32-strip
W32_WINDRES ?= i686-w64-mingw32-windres

# This is a thing because of bad defaults.
W32_SDL2_CFG ?= -I/usr/local/i686-w64-mingw32/include -L/usr/local/i686-w64-mingw32/lib

W32_CFLAGS ?= -Os -static-libgcc -Wno-multichar $(W32_SDL2_CFG)
W32_CFLAGS_LTO ?= $(W32_CFLAGS) -flto
W32_CFLAGS_DLL ?= $(W32_CFLAGS_LTO) -shared
W32_CFLAGS_EXE ?= $(W32_CFLAGS_LTO)
W32_CFLAGS_GUI ?= $(W32_CFLAGS_LTO) -mwindows
W32_CFLAGS_SDL ?= $(W32_CFLAGS) -mwindows

# Host
# (note: in the MSYS2 environment, these will use Cygwin!)
CC ?= cc
STRIP ?= strip

# Independent
NASM ?= nasm

