#!/usr/bin/env lua
-- libkc3ds - General-purpose Rust library for C3/DS
-- Written starting in 2024 by contributors (see CREDITS.txt)
-- To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
-- You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

print("pub(crate) const LINEAR_LIGHT_DITHER_POWER_TABLE: &[u16] = &[")
for i = 0, 255 do
    local tmp = i / 255.0
    tmp = math.floor((tmp ^ 2.15) * 65535)
    print("    " .. tmp .. ",")
end
print("];")
