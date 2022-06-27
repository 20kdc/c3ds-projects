all: caosproxy/caosprox.exe caosproxy/cpxinvrt.exe
rel: caosproxy/caosprox.exe caosproxy/cpxinvrt.exe

INTERMEDIATES += caosproxy/caosprox.exe caosproxy/cpxinvrt.exe caosproxy/w32/caospres.o

CAOSPROX_BODY := caosproxy/w32/cpxservg.c caosproxy/w32/cpxservi.c caosproxy/w32/caospres.o
CPXINVRT_BODY := caosproxy/w32/cpxinvrt.c caosproxy/w32/caospres.o

caosproxy/caosprox.exe: $(CAOSPROX_BODY) caosproxy/w32/libcpx.h
	$(COMPILER) $(COMPILE_FLAGS_GUI) $(CAOSPROX_BODY) -o $@ -lws2_32
	$(STRIP) $@

caosproxy/w32/caospres.o: caosproxy/w32/caospres.rc caosproxy/w32/caosprox.ico
	$(WINDRES) caosproxy/w32/caospres.rc caosproxy/w32/caospres.o

caosproxy/cpxinvrt.exe: $(CPXINVRT_BODY) caosproxy/w32/libcpx.h
	$(COMPILER) $(COMPILE_FLAGS_EXE) $(CPXINVRT_BODY) -o $@ -lws2_32 -lshlwapi
	$(STRIP) $@

