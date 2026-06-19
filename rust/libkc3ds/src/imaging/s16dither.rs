// libkc3ds - General-purpose Rust library for C3/DS
// Written starting in 2024 by contributors (see CREDITS.txt)
// To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
// You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

use super::*;

fn dither(
    method: &dyn BitDitherMethod,
    channel: Raster<u8>,
    target_bits: u8,
    is_alpha: bool,
) -> Raster<u8> {
    if target_bits == 8 {
        channel
    } else {
        method.run(channel, target_bits, is_alpha)
    }
}

/// Dithers for sprites given bit-dithering algorithms.
pub fn spr(
    image: &Raster<ARGB32>,
    model: &dyn ColourModel,
    colour: &dyn BitDitherMethod,
    alpha: &dyn BitDitherMethod,
) -> Raster<Pixel> {
    if model.dither_bypass() {
        return image.map(&mut |_, _, v| CM_ARGB32.encode(v));
    }

    let raster_alpha = ARGB32::raster_a(image);
    // should be 0x00 / 0xFF
    let alpha_stage = dither(alpha, raster_alpha, 1, true);
    let mut rgb_stage = blk(image, model, colour);
    rgb_stage.map_inplace(&mut |x, y, v| {
        let alpha = alpha_stage.pixel(x, y) >= 0x80;
        if alpha {
            if model.is_sprite_transparency(v as Pixel) {
                model.sprite_nudge_value()
            } else {
                v
            }
        } else {
            0
        }
    });
    rgb_stage
}

/// Dithers for backgrounds given bit-dithering algorithms.
pub fn blk(
    image: &Raster<ARGB32>,
    model: &dyn ColourModel,
    colour: &dyn BitDitherMethod,
) -> Raster<Pixel> {
    if model.dither_bypass() {
        return image.map(&mut |_, _, v| CM_ARGB32.encode(v));
    }

    let res_r = dither(colour, ARGB32::raster_r(image), model.r_bits(), false);
    let res_g = dither(colour, ARGB32::raster_g(image), model.g_bits(), false);
    let res_b = dither(colour, ARGB32::raster_b(image), model.b_bits(), false);
    Raster::generate(image.width(), image.height(), &mut |x, y| {
        let pix_r = res_r.pixel(x, y);
        let pix_g = res_g.pixel(x, y);
        let pix_b = res_b.pixel(x, y);
        model.encode(ARGB32(255, RGB24(pix_r, pix_g, pix_b)))
    })
}
