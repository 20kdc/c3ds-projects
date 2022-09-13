# Attempt to auto-detect OS.
# Note that MSYS2 should be considered a "Cygwin" host cross-compiling to Windows.

ifeq ($(shell uname),Linux)
HOST_LINUX ?= 1
HOST_GODOT ?= 1
endif

# Assume we don't have Godot but leave it open for an override
HOST_LINUX ?= 0
HOST_GODOT ?= 0

# Windows
W32_CC ?= i686-w64-mingw32-gcc
W32_STRIP ?= i686-w64-mingw32-strip
ifeq ($(shell uname),Linux)
W32_WINDRES ?= i686-w64-mingw32-windres
else
W32_WINDRES ?= windres
endif

W32_CFLAGS ?= -Os -static-libgcc -Wno-multichar $(W32_SDL2_CFG)
W32_CFLAGS_LTO ?= $(W32_CFLAGS) -flto
W32_CFLAGS_DLL ?= $(W32_CFLAGS_LTO) -shared
W32_CFLAGS_EXE ?= $(W32_CFLAGS_LTO)
W32_CFLAGS_GUI ?= $(W32_CFLAGS_LTO) -mwindows

# Host
# (note: in the MSYS2 environment, these will use Cygwin!)
CC ?= cc
STRIP ?= strip

# Independent
NASM ?= nasm

