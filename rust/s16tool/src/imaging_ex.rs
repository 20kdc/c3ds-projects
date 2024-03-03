// libkc3ds - General-purpose Rust library for C3/DS
// Written starting in 2024 by contributors (see CREDITS.txt)
// To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
// You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

use libkc3ds::imaging::*;

/// Imports an image
pub fn import(name: &str) -> Raster<ARGB32> {
    // nyi
    Raster::new(0, 0)
}

/// Exports an image
pub fn export_rgb(name: &str, image: &Raster<RGB24>, bmp: bool) {
    // nyi
}

/// Exports an image
pub fn export_argb(name: &str, image: &Raster<ARGB32>, bmp: bool) {
    // nyi
}

/// Exports an image
pub fn export_spr(name: &str, image: &Raster<u16>, cm: &dyn ColourModel<u16>, bmp: bool) {
    export_argb(name, &cm.decode_raster_spr(image), bmp);
}

/// Exports an image
pub fn export_blk(name: &str, image: &Raster<u16>, cm: &dyn ColourModel<u16>, bmp: bool) {
    export_rgb(name, &cm.decode_raster_blk(image), bmp);
}
