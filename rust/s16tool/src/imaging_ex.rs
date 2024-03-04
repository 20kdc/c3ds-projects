// libkc3ds - General-purpose Rust library for C3/DS
// Written starting in 2024 by contributors (see CREDITS.txt)
// To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
// You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

use libkc3ds::imaging::*;

/// Imports an image
pub fn import(name: &str) -> Raster<ARGB32> {
    let dynimg = image::load_from_memory(&std::fs::read(name).unwrap()).unwrap();
    let img8 = dynimg.into_rgba8();
    Raster::generate(
        img8.width() as usize,
        img8.height() as usize,
        &mut |x, y| {
            let pixel = img8.get_pixel(x as u32, y as u32).0;
            ARGB32(pixel[3], RGB24(pixel[0], pixel[1], pixel[2]))
        },
    )
}

/// Exports an image
pub fn export_rgb(name: &str, image: &Raster<RGB24>, bmp: bool) {
    let mut img = image::RgbImage::new(image.width() as u32, image.height() as u32);
    img.enumerate_pixels_mut().for_each(|(x, y, p)| {
        let px = image.pixel(x as usize, y as usize);
        *p = image::Rgb([px.0, px.1, px.2]);
    });
    img.save_with_format(
        name,
        if bmp {
            image::ImageFormat::Bmp
        } else {
            image::ImageFormat::Png
        },
    )
    .unwrap();
}

/// Exports an image
pub fn export_argb(name: &str, image: &Raster<ARGB32>, bmp: bool) {
    let mut img = image::RgbaImage::new(image.width() as u32, image.height() as u32);
    img.enumerate_pixels_mut().for_each(|(x, y, p)| {
        let px = image.pixel(x as usize, y as usize);
        *p = image::Rgba([px.1 .0, px.1 .1, px.1 .2, px.0]);
    });
    img.save_with_format(
        name,
        if bmp {
            image::ImageFormat::Bmp
        } else {
            image::ImageFormat::Png
        },
    )
    .unwrap();
}

/// Exports an image
pub fn export_spr(name: &str, image: &Raster<u16>, cm: &dyn ColourModel<u16>, bmp: bool) {
    export_argb(name, &cm.decode_raster_spr(image), bmp);
}

/// Exports an image
pub fn export_blk(name: &str, image: &Raster<u16>, cm: &dyn ColourModel<u16>, bmp: bool) {
    export_rgb(name, &cm.decode_raster_blk(image), bmp);
}
