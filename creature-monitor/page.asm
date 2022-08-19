bits 32

section .data

global cm_page_start
global cm_page_len
global _cm_page_start
global _cm_page_len

cm_page_start:
_cm_page_start:
incbin "creature-monitor/page.bmp"
cm_page_len:
_cm_page_len:
dd cm_page_len - cm_page_start

