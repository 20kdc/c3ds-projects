bits 32

section .text

global _ddraw_hook_table
global _enforce_window_xy
global _april_fools_24

extern _specialFixWindowRect@4

; Null-terminated list of hook suite/string pairs.
_ddraw_hook_table:
dd suite_2p286_b195cd, suite_2p286_b195cd_name ; DS   (with check)
dd suite_2p286_b195, suite_2p286_b195_name     ; DS   (no check)
dd suite_1p162cd, suite_1p162cd_name           ; C3u2 (with check)
dd suite_1p162, suite_1p162_name               ; C3u2 (no check)
dd suite_1p158cd, suite_1p158cd_name           ; C3u1 (with check)
dd suite_1p158, suite_1p158_name               ; C3u1 (no check)
dd suite_1p147cd, suite_1p147cd_name           ; C3   (with check)
dd suite_1p147, suite_1p147_name               ; C3   (no check)
dd 0, 0

suite_2p286_b195cd_name:
db "Engine 2.286 B195 - Docking Station (with check)", 0

suite_2p286_b195_name:
db "Engine 2.286 B195 - Docking Station (no check)", 0

suite_1p162cd_name:
db "Engine 1.162 - Creatures 3 Update 2 (with check)", 0

suite_1p162_name:
db "Engine 1.162 - Creatures 3 Update 2 (no check)", 0

suite_1p158cd_name:
db "Engine 1.158 - Creatures 3 Update 1 (with check)", 0

suite_1p158_name:
db "Engine 1.158 - Creatures 3 Update 1 (no check)", 0

suite_1p147cd_name:
db "Engine 1.147 - Creatures 3 (with check)", 0

suite_1p147_name:
db "Engine 1.147 - Creatures 3 (no check)", 0

; These tables contain sets of 3: the absolute address, a pointer to the call target, and a pointer to the expected 5 bytes.
suite_2p286_b195cd:
dd 0x00556030, cfcd_hook_code, cfcd_hook_test_ds
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
; Fix user.cfg corruption bug by not trusting the metrics
dd 0x0057DC6D, special_usercfg_code, special_usercfg_test
; done!
dd 0, 0, 0

suite_1p162cd:
dd 0x005550B0, cfcd_hook_code, cfcd_hook_test
suite_1p162:
; CreateFullscreenDisplaySurfaces
dd 0x0047D87B, cs_hook_code, cs_hook_ecx_test
dd 0x0047D8EC, cs_hook_code, cs_hook_edx_test
dd 0x0047D90C, cs_hook_code, cs_hook_edx_test
; CreateWindowedDisplaySurfaces
dd 0x0047DB1D, cs_hook_code, cs_hook_ecx_test
dd 0x0047DB61, cs_hook_code, cs_hook_edx_test
; FlipScreenHorizontally
dd 0x0047E6F3, cs_hook_code, cs_hook_edx_test
dd 0x0047E70F, cs_hook_code, cs_hook_edx_test
; CreateSurface
dd 0x00481A32, cs_hook_code, cs_hook_edx_test
dd 0x00481A53, cs_hook_code, cs_hook_edx_test
dd 0, 0, 0

suite_1p158cd:
dd 0x00554BF0, cfcd_hook_code, cfcd_hook_test
suite_1p158:
; CreateFullscreenDisplaySurfaces
dd 0x0047DA3B, cs_hook_code, cs_hook_ecx_test
dd 0x0047DAAC, cs_hook_code, cs_hook_edx_test
dd 0x0047DACC, cs_hook_code, cs_hook_edx_test
; CreateWindowedDisplaySurfaces
dd 0x0047DCDD, cs_hook_code, cs_hook_ecx_test
dd 0x0047DD21, cs_hook_code, cs_hook_edx_test
; FlipScreenHorizontally
dd 0x0047E8B3, cs_hook_code, cs_hook_edx_test
dd 0x0047E8CF, cs_hook_code, cs_hook_edx_test
; CreateSurface
dd 0x00481BE2, cs_hook_code, cs_hook_edx_test
dd 0x00481C03, cs_hook_code, cs_hook_edx_test
dd 0, 0, 0

suite_1p147cd:
dd 0x00553100, cfcd_hook_code, cfcd_hook_test
suite_1p147:
; CreateFullscreenDisplaySurfaces
dd 0x0047BA2B, cs_hook_code, cs_hook_ecx_test
dd 0x0047BA9C, cs_hook_code, cs_hook_edx_test
dd 0x0047BABC, cs_hook_code, cs_hook_edx_test
; CreateWindowedDisplaySurfaces
dd 0x0047BCCD, cs_hook_code, cs_hook_ecx_test
dd 0x0047BD11, cs_hook_code, cs_hook_edx_test
; FlipScreenHorizontally
dd 0x0047C8A3, cs_hook_code, cs_hook_edx_test
dd 0x0047C8BF, cs_hook_code, cs_hook_edx_test
; CreateSurface
dd 0x0047FBD2, cs_hook_code, cs_hook_edx_test
dd 0x0047FBF3, cs_hook_code, cs_hook_edx_test
dd 0, 0, 0

; sanity check strings
cs_hook_ecx_test:
db 0xFF, 0x51, 0x18, 0x85, 0xC0
; PRESS S, THEN PASTE: FF 51 18 85 C0
cs_hook_edx_test:
db 0xFF, 0x52, 0x18, 0x85, 0xC0
; PRESS S, THEN PASTE: FF 52 18 85 C0
cfcd_hook_test:
db 0x6A, 0xFF, 0x64, 0xA1, 0x00
; PRESS S, THEN PASTE: 6A FF 64 A1 00
cfcd_hook_test_ds:
db 0x55, 0x8B, 0xEC, 0x64, 0xA1
; PRESS S, THEN PASTE: 55 8B EC 64 A1

; actual code

cs_hook_code:

; ATTENTION: What this code does is wrap a IDirectDraw.CreateSurface call ([e*x + 0x18]) & patch in the RGB565 pixel format.
; It also includes the "test eax, eax" that follows, as otherwise 5 bytes would not be consumed properly.
; See the test strings above for expected code to replace.

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

; so some explanation of what's going on here:
; the DevThing patches "elegantly" reverse the sense of the JZ/JNZ test
; so one needs to account for that in this patch so it works either way
cfcd_hook_code:
pop eax ; this is the address of the function we're replacing
pop eax ; this is the address of 84 C0 0F 84/85 .. .. .. ..
add eax, [eax + 4]
add eax, 8
jmp eax

; the window parameter validity test doesn't work for some extreme cases, i.e.
; WindowBottom -31976
; WindowLeft -32000
; WindowRight -31840
; WindowTop -32000
; this can cause crashes when the backbuffer never shows up
; interestingly C3 isn't susceptible?
; anyways, presumably this is a rogue compositor shunting the window into the shadow realm (this was seen on what I believe is Mint Linux)
special_usercfg_test:
db 0x7E, 0x1D, 0x2B, 0xCE, 0x85
special_usercfg_code:
; remove old return address
pop eax
; locate and push rectangle location
lea eax, [ebp - 0x20]
push eax
push 0x57DCA0
jmp _specialFixWindowRect@4

; -- Settings --
align 16
db "                "
db "                "

db "LimitWindowXY:"
_enforce_window_xy:
db "Y "

db "                "
db "                "
; --

align 16
db "                "
db "                "

db "RegCode:"
_april_fools_24:
db "1234 "

db "                "
db "                "

