CREATURE_MONITOR_SOURCES := creature-monitor/main.cpp creature-monitor/libcm.cpp
CREATURE_MONITOR_SOURCES += creature-monitor/state_inforead.cpp creature-monitor/state_selector.cpp creature-monitor/state_chem.cpp creature-monitor/state_brain.cpp
CREATURE_MONITOR_SOURCES += creature-monitor/cpx.cpp

CREATURE_MONITOR_HEADERS := creature-monitor/main.h creature-monitor/cpx.h creature-monitor/libcm.h

ifeq ($(HOST_SDL2),1)
# host build
all: creature-monitor/creature-monitor
rel: creature-monitor/creature-monitor
INTERMEDIATES += creature-monitor/creature-monitor

creature-monitor/creature-monitor: $(CREATURE_MONITOR_SOURCES) $(CREATURE_MONITOR_HEADERS) creature-monitor/page-elf64.o
	$(CC) -fno-rtti -fno-exceptions $(CREATURE_MONITOR_SOURCES) creature-monitor/page-elf64.o -lSDL2 -lSDL2_net -o $@

endif

ifeq ($(W32_SDL2),1)
# w32 build
all: creature-monitor/creature-monitor.exe creature-monitor/SDL2.dll creature-monitor/SDL2_net.dll
rel: creature-monitor/creature-monitor.exe creature-monitor/SDL2.dll creature-monitor/SDL2_net.dll
INTERMEDIATES += creature-monitor/creature-monitor.exe creature-monitor/SDL2.dll creature-monitor/SDL2_net.dll

creature-monitor/creature-monitor.exe: $(CREATURE_MONITOR_SOURCES) $(CREATURE_MONITOR_HEADERS) creature-monitor/page-coff.o
	$(W32_CC) -fno-rtti -fno-exceptions $(W32_CFLAGS_SDL) $(CREATURE_MONITOR_SOURCES) creature-monitor/page-coff.o -o $@ -lSDL2 -lSDL2_net
	$(W32_STRIP) $@

creature-monitor/SDL2.dll:
	cp /usr/local/i686-w64-mingw32/bin/SDL2.dll $@ || cp /usr/i686-w64-mingw32/bin/SDL2.dll $@ || cp /mingw32/bin/SDL2.dll $@

creature-monitor/SDL2_net.dll:
	cp /usr/local/i686-w64-mingw32/bin/SDL2_net.dll $@ || cp /usr/i686-w64-mingw32/bin/SDL2_net.dll $@ || cp /mingw32/bin/SDL2_net.dll $@

endif

INTERMEDIATES += creature-monitor/page-elf64.o creature-monitor/page-coff.o
creature-monitor/page-elf64.o: creature-monitor/page.asm creature-monitor/page.bmp
	$(NASM) -felf64 $< -o $@
creature-monitor/page-coff.o: creature-monitor/page.asm creature-monitor/page.bmp
	$(NASM) -fwin32 $< -o $@

