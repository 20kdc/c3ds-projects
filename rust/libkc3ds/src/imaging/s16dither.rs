// libkc3ds - General-purpose Rust library for C3/DS
// Written starting in 2024 by contributors (see CREDITS.txt)
// To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
// You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

use super::*;

/// Dithers for sprites given bit-dithering algorithms.
pub fn spr(image: &Raster<ARGB32>, model: &ColourModelRGB16, colour: &dyn BitDitherMethod, alpha: &dyn BitDitherMethod) -> Raster<u16> {
    let raster_alpha = ARGB32::raster_a(image);
    // should be 0x00 / 0xFF
    let alpha_stage = alpha.run(raster_alpha, 1);
    let mut rgb_stage = blk(&ARGB32::raster_rgb(image), model, colour);
    rgb_stage.map_inplace(&mut |x, y, v| {
        let alpha = alpha_stage.pixel(x, y) >= 0x80;
        if alpha {
            if model.opaque(v) {
                v
            } else {
                model.nudge
            }
        } else {
            0
        }
    });
    rgb_stage
}

/// Dithers for backgrounds given bit-dithering algorithms.
pub fn blk(image: &Raster<RGB24>, model: &ColourModelRGB16, colour: &dyn BitDitherMethod) -> Raster<u16> {
    let (raster_r, raster_g, raster_b) = RGB24::separate_channels(image);
    let res_r = colour.run(raster_r, model.r_bits.bits() as u8);
    let res_g = colour.run(raster_g, model.g_bits.bits() as u8);
    let res_b = colour.run(raster_b, model.b_bits.bits() as u8);
    Raster::generate(image.width(), image.height(), &mut |x, y| {
        let pix_r = res_r.pixel(x, y);
        let pix_g = res_g.pixel(x, y);
        let pix_b = res_b.pixel(x, y);
        model.encode(RGB24(pix_r, pix_g, pix_b))
    })
}
