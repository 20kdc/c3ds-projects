ifeq ($(HOST_SDL2),1)
# host build
all: creature-monitor/creature-monitor
rel: creature-monitor/creature-monitor
INTERMEDIATES += creature-monitor/creature-monitor

creature-monitor/creature-monitor: creature-monitor/main.c
	$(CC) creature-monitor/main.c -lSDL2 -lSDL2_net -o $@

endif

ifeq ($(W32_SDL2),1)
# w32 build
all: creature-monitor/creature-monitor.exe creature-monitor/SDL2.dll creature-monitor/SDL2_net.dll
rel: creature-monitor/creature-monitor.exe creature-monitor/SDL2.dll creature-monitor/SDL2_net.dll
INTERMEDIATES += creature-monitor/creature-monitor.exe creature-monitor/SDL2.dll creature-monitor/SDL2_net.dll

creature-monitor/creature-monitor.exe: creature-monitor/main.c
	$(W32_CC) $(W32_CFLAGS_SDL) creature-monitor/main.c -o $@ -lSDL2 -lSDL2_net

creature-monitor/SDL2.dll:
	cp /usr/local/i686-w64-mingw32/bin/SDL2.dll $@ || cp /usr/i686-w64-mingw32/bin/SDL2.dll $@ || cp /mingw32/bin/SDL2.dll $@

creature-monitor/SDL2_net.dll:
	cp /usr/local/i686-w64-mingw32/bin/SDL2_net.dll $@ || cp /usr/i686-w64-mingw32/bin/SDL2_net.dll $@ || cp /mingw32/bin/SDL2_net.dll $@

endif

