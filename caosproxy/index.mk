all: bin/caosprox.exe

INTERMEDIATES += bin/caosprox.exe bin/caospres.o

bin/caosprox.exe: caosproxy/cpxservg.c caosproxy/cpxservi.c bin/caospres.o
	$(COMPILER) $(COMPILE_FLAGS_DLL) $^ -o $@ -lws2_32
	$(STRIP) $@

bin/caospres.o: caosproxy/caospres.rc caosproxy/caosprox.ico
	$(WINDRES) caosproxy/caospres.rc bin/caospres.o

