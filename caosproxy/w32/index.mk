all: caosproxy/caosprox.exe caosproxy/cpxinvrt.exe
CAOSPROX_REL += caosproxy/caosprox.exe caosproxy/cpxinvrt.exe

INTERMEDIATES += caosproxy/caosprox.exe caosproxy/cpxinvrt.exe caosproxy/w32/caospres.o

CAOSPROX_BODY := caosproxy/w32/cpxservg.c caosproxy/w32/cpxservi.c caosproxy/w32/caospres.o
CPXINVRT_BODY := caosproxy/w32/cpxinvrt.c caosproxy/w32/caospres.o

caosproxy/caosprox.exe: $(CAOSPROX_BODY) caosproxy/w32/libcpx.h
	$(W32_CC) $(W32_CFLAGS_GUI) $(CAOSPROX_BODY) -o $@ -lws2_32
	$(W32_STRIP) $@

caosproxy/w32/caospres.o: caosproxy/w32/caospres.rc caosproxy/w32/caosprox.ico
	$(W32_WINDRES) caosproxy/w32/caospres.rc caosproxy/w32/caospres.o

caosproxy/cpxinvrt.exe: $(CPXINVRT_BODY) caosproxy/w32/libcpx.h
	$(W32_CC) $(W32_CFLAGS_EXE) $(CPXINVRT_BODY) -o $@ -lws2_32 -lshlwapi
	$(W32_STRIP) $@

