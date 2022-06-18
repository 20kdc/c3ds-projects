all: caosproxy/caosprox.exe
rel: caosproxy/caosprox.exe

INTERMEDIATES += caosproxy/caosprox.exe caosproxy/w32/caospres.o

caosproxy/caosprox.exe: caosproxy/w32/cpxservg.c caosproxy/w32/cpxservi.c caosproxy/w32/caospres.o
	$(COMPILER) $(COMPILE_FLAGS_GUI) $^ -o $@ -lws2_32
	$(STRIP) $@

caosproxy/w32/caospres.o: caosproxy/w32/caospres.rc caosproxy/w32/caosprox.ico
	$(WINDRES) caosproxy/w32/caospres.rc caosproxy/w32/caospres.o

