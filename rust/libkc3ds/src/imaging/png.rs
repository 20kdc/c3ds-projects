// libkc3ds - General-purpose Rust library for C3/DS
// Written starting in 2024 by contributors (see CREDITS.txt)
// To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
// You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

// Wrappers around image crate to bridge formats.

use super::{ColourModel, Pixel, Raster, RasterishObj, ARGB32, CM_ARGB32, RGB24};

/// Creates a [CM_ARGB32] raster from a PNG
pub fn s32png_decode(png_data: &[u8]) -> Result<Raster<Pixel>, String> {
    let image = image::load_from_memory_with_format(png_data, image::ImageFormat::Png)
        .map_err(|err| format!("S32 PNG: {:?}", err))?;
    let img8 = image.into_rgba8();
    Ok(Raster::generate(
        img8.width() as usize,
        img8.height() as usize,
        &mut |x, y| {
            let pixel = img8.get_pixel(x as u32, y as u32).0;
            CM_ARGB32.encode(ARGB32(pixel[3], RGB24(pixel[0], pixel[1], pixel[2])))
        },
    ))
}

/// Given a [CM_ARGB32] raster, encodes a PNG.
pub fn s32png_encode(raster: &Raster<Pixel>) -> Vec<u8> {
    let conv = image::RgbaImage::from_fn(raster.width() as u32, raster.height() as u32, |x, y| {
        let pix = CM_ARGB32.decode(raster.pixel(x as usize, y as usize), false);
        image::Rgba([pix.1 .0, pix.1 .1, pix.1 .2, pix.0])
    });
    let mut cursorish = std::io::Cursor::new(Vec::new());
    conv.write_to(&mut cursorish, image::ImageFormat::Png)
        .unwrap();
    cursorish.into_inner()
}
