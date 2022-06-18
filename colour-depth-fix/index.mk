all: colour-depth-fix/ddrhk.dll colour-depth-fix/ddrhk.exe
rel: colour-depth-fix/ddrhk.dll colour-depth-fix/ddrhk.exe colour-depth-fix/README.md

INTERMEDIATES += colour-depth-fix/ddrhk.dll colour-depth-fix/dscf_a.o colour-depth-fix/ddrhk.exe

colour-depth-fix/ddrhk.dll: colour-depth-fix/dscf.c colour-depth-fix/dscf_a.o
	$(COMPILER) $(COMPILE_FLAGS_DLL) $^ -o $@

colour-depth-fix/dscf_a.o: colour-depth-fix/dscf_a.asm
	$(NASM) -fwin32 $< -o $@

colour-depth-fix/ddrhk.exe: colour-depth-fix/ddrhk.c
	$(COMPILER) $(COMPILE_FLAGS_GUI) $^ -o $@

