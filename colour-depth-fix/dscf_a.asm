bits 32

section .text

global _ddraw_hook_table

; Null-terminated list of hook suite/string pairs.
_ddraw_hook_table:
dd suite_2p286_b195, suite_2p286_b195_name
dd 0, 0

suite_2p286_b195_name:
db "Engine 2.286 B195 - Docking Station", 0

; These tables contain sets of 3: the absolute address, a pointer to the jump target, and a pointer to the expected 5 bytes.
suite_2p286_b195:
; CreateFullscreenDisplaySurfaces
dd 0x00472FE1, cs_hook_code, cs_hook_ecx_test
dd 0x0047304B, cs_hook_code, cs_hook_edx_test
dd 0x00473069, cs_hook_code, cs_hook_edx_test
; CreateWindowedDisplaySurfaces
dd 0x0047327B, cs_hook_code, cs_hook_ecx_test
dd 0x004732BA, cs_hook_code, cs_hook_edx_test
; FlipScreenHorizontally
dd 0x004737B9, cs_hook_code, cs_hook_edx_test
dd 0x004737D6, cs_hook_code, cs_hook_edx_test
; CreateSurface
dd 0x0047626E, cs_hook_code, cs_hook_edx_test
dd 0x0047628C, cs_hook_code, cs_hook_edx_test
; done!
dd 0, 0, 0

; sanity check strings
cs_hook_ecx_test:
db 0xFF, 0x51, 0x18, 0x85, 0xC0
cs_hook_edx_test:
db 0xFF, 0x52, 0x18, 0x85, 0xC0

; actual code

cs_hook_code:

; ATTENTION: What this code does is wrap a IDirectDraw.CreateSurface call ([e*x + 0x18]) & patch in the RGB565 pixel format.

; frame: (ret, this, p1, p2, p3)

; Firstly, get the vtable pointer reliably.
; It may be in different registers for different calls.
; So grab it again and store it in ECX.
mov eax, [esp + 0x04]
mov ecx, [eax]

; frame: (ret, this, p1, p2, p3)

mov eax, [esp + 0x10]
push eax
mov eax, [esp + 0x10]
push eax
mov eax, [esp + 0x10]
push eax
mov eax, [esp + 0x10]
push eax

; frame: (this, p1, p2, p3, ret, this, p1, p2, p3)

mov eax, [esp + 0x10]
mov [esp + 0x20], eax

; frame: (this, p1, p2, p3, ret, this, p1, p2, ret)

mov eax, [esp + 0x04]
or [eax + 0x04], dword 0x1000 ; DDSD_PIXELFORMAT
add eax, 72
mov [eax + 0x00], dword 0x20 ; dwSize
mov [eax + 0x04], dword 0x0040 ; dwFlags
mov [eax + 0x08], dword 0x0000 ; dwFourCC
mov [eax + 0x0C], dword 0x0010 ; dwRGBBitCount
mov [eax + 0x10], dword 0xF800 ; dwRBitMask
mov [eax + 0x14], dword 0x07E0 ; dwGBitMask
mov [eax + 0x18], dword 0x001F ; dwBBitMask
mov [eax + 0x1C], dword 0x0000 ; dwRGBAlphaBitMask

call [ecx + 0x18]

; frame: (ret, this, p1, p2, ret)

add esp, 0x10

test eax, eax
ret

