all: colour-depth-fix/ddrhk.dll colour-depth-fix/ddrhk.exe
rel: colour-depth-fix/ddrhk.dll colour-depth-fix/ddrhk.exe colour-depth-fix/README.md

INTERMEDIATES += colour-depth-fix/ddrhk.dll colour-depth-fix/dscf_a.o colour-depth-fix/ddrhk.exe

colour-depth-fix/ddrhk.dll: colour-depth-fix/dscf.c colour-depth-fix/dscf_a.o
	$(W32_CC) $(W32_CFLAGS_DLL) $^ -o $@
	$(W32_STRIP) $@

colour-depth-fix/dscf_a.o: colour-depth-fix/dscf_a.asm
	$(NASM) -fwin32 $< -o $@

colour-depth-fix/ddrhk.exe: colour-depth-fix/ddrhk.c
	$(W32_CC) $(W32_CFLAGS_GUI) $^ -o $@
	$(W32_STRIP) $@

