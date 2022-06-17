all: bin/ddrhk.dll bin/ddrhk.exe
rel: bin/ddrhk.dll bin/ddrhk.exe colour-depth-fix/README.md

INTERMEDIATES += bin/ddrhk.dll bin/dscf_a.obj bin/ddrhk.exe

bin/ddrhk.dll: colour-depth-fix/dscf.c bin/dscf_a.obj
	$(COMPILER) $(COMPILE_FLAGS_DLL) $^ -o $@

bin/dscf_a.obj: colour-depth-fix/dscf_a.asm
	$(NASM) -fwin32 $< -o $@

bin/ddrhk.exe: colour-depth-fix/ddrhk.c
	$(COMPILER) $(COMPILE_FLAGS_GUI) -Os -flto -static-libgcc -Wno-multichar -mwindows $^ -o $@

