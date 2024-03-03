// libkc3ds - General-purpose Rust library for C3/DS
// Written starting in 2024 by contributors (see CREDITS.txt)
// To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
// You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

use super::{Rasterish, RasterishMut, RasterishObj, RasterishMutObj, RasterRegion, RasterRegionMut};

/// Vec-ized raster of pixels.
#[derive(Clone)]
pub struct Raster<P: Copy + Sized> {
    width: usize,
    height: usize,
    pixels: Vec<P>,
}

impl<P: Copy + Sized + Default> Raster<P> {
    /// New Raster.
    pub fn new(width: usize, height: usize) -> Raster<P> {
        Self::new_filled(width, height, P::default())
    }

    /// New Raster with the given fill colour.
    pub fn new_filled(width: usize, height: usize, fill_colour: P) -> Raster<P> {
        let total = width * height;
        let mut vec = Vec::with_capacity(total);
        vec.resize(total, fill_colour);
        Raster {
            width,
            height,
            pixels: vec,
        }
    }

    /// Generate Raster from a function.
    /// If you don't particularly care that much about performance, or you think the optimizer will handle it anyway, you can implement a lot of ops this way.
    pub fn generate<F: FnMut(usize, usize) -> P>(
        width: usize,
        height: usize,
        f: &mut F,
    ) -> Raster<P> {
        let mut vec = Vec::with_capacity(width * height);
        for y in 0..height {
            for x in 0..width {
                vec.push(f(x, y));
            }
        }
        Raster {
            width,
            height,
            pixels: vec,
        }
    }
}

impl<P: Copy + Sized + Default> RasterishObj<P> for Raster<P> {
    #[inline]
    fn width(&self) -> usize {
        self.width
    }
    #[inline]
    fn height(&self) -> usize {
        self.height
    }
    #[inline]
    fn row<'a>(&'a self, y: usize) -> &'a [P] {
        let pos = y * self.width;
        &self.pixels[pos..pos + self.width]
    }
    #[inline]
    fn region<'x>(
        &'x self,
        x: usize,
        y: usize,
        width: usize,
        height: usize,
    ) -> RasterRegion<'x, P> {
        RasterRegion {
            backing: self,
            x,
            y,
            width,
            height,
        }
    }
}

impl<P: Copy + Sized + Default> RasterishMutObj<P> for Raster<P> {
    #[inline]
    fn row_mut<'a>(&'a mut self, y: usize) -> &'a mut [P] {
        let pos = y * self.width;
        &mut self.pixels[pos..pos + self.width]
    }
    #[inline]
    fn region_mut<'x>(
        &'x mut self,
        x: usize,
        y: usize,
        width: usize,
        height: usize,
    ) -> RasterRegionMut<'x, P> {
        RasterRegionMut {
            backing: self,
            x,
            y,
            width,
            height,
        }
    }
}

impl<P: Copy + Sized + Default> Rasterish<P> for Raster<P> {}

impl<P: Copy + Sized + Default> RasterishMut<P> for Raster<P> {}

/// Compile-time-defined fixed-size raster.
#[derive(Clone, Copy)]
pub struct RasterTile<P: Copy + Sized + Default, const WIDTH: usize, const HEIGHT: usize>(pub [[P; WIDTH]; HEIGHT]);

impl<P: Copy + Sized + Default, const WIDTH: usize, const HEIGHT: usize> Default for RasterTile<P, WIDTH, HEIGHT> {
    #[inline]
    fn default() -> Self {
        Self([[P::default(); WIDTH]; HEIGHT])
    }
}

impl<P: Copy + Sized + Default, const WIDTH: usize, const HEIGHT: usize> RasterishObj<P> for RasterTile<P, WIDTH, HEIGHT> {
    #[inline]
    fn width(&self) -> usize {
        WIDTH
    }
    #[inline]
    fn height(&self) -> usize {
        HEIGHT
    }
    #[inline]
    fn row<'a>(&'a self, y: usize) -> &'a [P] {
        &self.0[y]
    }
    #[inline]
    fn region<'x>(
        &'x self,
        x: usize,
        y: usize,
        width: usize,
        height: usize,
    ) -> RasterRegion<'x, P> {
        RasterRegion {
            backing: self,
            x,
            y,
            width,
            height,
        }
    }
}

impl<P: Copy + Sized + Default, const WIDTH: usize, const HEIGHT: usize> RasterishMutObj<P> for RasterTile<P, WIDTH, HEIGHT> {
    #[inline]
    fn row_mut<'a>(&'a mut self, y: usize) -> &'a mut [P] {
        &mut self.0[y]
    }
    #[inline]
    fn region_mut<'x>(
        &'x mut self,
        x: usize,
        y: usize,
        width: usize,
        height: usize,
    ) -> RasterRegionMut<'x, P> {
        RasterRegionMut {
            backing: self,
            x,
            y,
            width,
            height,
        }
    }
}

impl<P: Copy + Sized + Default, const WIDTH: usize, const HEIGHT: usize> Rasterish<P> for RasterTile<P, WIDTH, HEIGHT> {}
impl<P: Copy + Sized + Default, const WIDTH: usize, const HEIGHT: usize> RasterishMut<P> for RasterTile<P, WIDTH, HEIGHT> {}
