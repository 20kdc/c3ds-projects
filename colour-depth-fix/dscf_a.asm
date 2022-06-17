bits 32

section .text

global _ddraw_createsurface_hook_ecx_code
global _ddraw_createsurface_hook_ecx_test
global _ddraw_createsurface_hook_edx_code
global _ddraw_createsurface_hook_edx_test

; sanity check strings
_ddraw_createsurface_hook_ecx_test:
db 0xFF, 0x51, 0x18, 0x85, 0xC0
_ddraw_createsurface_hook_edx_test:
db 0xFF, 0x52, 0x18, 0x85, 0xC0

_ddraw_createsurface_hook_edx_code:
mov ecx, edx

_ddraw_createsurface_hook_ecx_code:

; ATTENTION: What this code does is wrap a IDirectDraw.CreateSurface call ([e*x + 0x18]) & patch in the RGB565 pixel format.

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

