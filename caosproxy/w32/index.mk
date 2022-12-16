CAOSPROX_W32_EXES := caosproxy/caosprox.exe caosproxy/caosprxc.exe
CAOSPROX_W32_EXES += caosproxy/cpxinvrt.exe
all: $(CAOSPROX_W32_EXES)

CAOSPROX_REL += $(CAOSPROX_W32_EXES)

INTERMEDIATES += $(CAOSPROX_W32_EXES)
INTERMEDIATES += caosproxy/w32/caospres.o

CAOSPROX_BODY := caosproxy/w32/cpxservg.c caosproxy/w32/cpxservi.c caosproxy/w32/cpxservl.c
CAOSPROX_BODY += caosproxy/w32/caospres.o caosproxy/w32/libcpx.c

CPXINVRT_BODY := caosproxy/w32/cpxinvrt.c
CPXINVRT_BODY += caosproxy/w32/caospres.o caosproxy/w32/libcpx.c

caosproxy/caosprox.exe: $(CAOSPROX_BODY) caosproxy/w32/libcpx.h caosproxy/w32/cpxservc.h
	$(W32_CC) $(W32_CFLAGS_GUI) $(CAOSPROX_BODY) -o $@ -lws2_32
	$(W32_STRIP) $@

caosproxy/caosprxc.exe: $(CAOSPROX_BODY) caosproxy/w32/libcpx.h caosproxy/w32/cpxservc.h
	$(W32_CC) $(W32_CFLAGS_EXE) $(CAOSPROX_BODY) -o $@ -lws2_32
	$(W32_STRIP) $@

caosproxy/w32/caospres.o: caosproxy/w32/caospres.rc caosproxy/w32/caosprox.ico
	$(W32_WINDRES) caosproxy/w32/caospres.rc caosproxy/w32/caospres.o

caosproxy/cpxinvrt.exe: $(CPXINVRT_BODY) caosproxy/w32/libcpx.h
	$(W32_CC) $(W32_CFLAGS_EXE) $(CPXINVRT_BODY) -o $@ -lws2_32 -lshlwapi
	$(W32_STRIP) $@

