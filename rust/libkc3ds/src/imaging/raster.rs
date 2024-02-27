// libkc3ds - General-purpose Rust library for C3/DS
// Written starting in 2024 by contributors (see CREDITS.txt)
// To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
// You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

/// Vec-ized raster of pixels.
#[derive(Clone)]
pub struct Raster<P: Copy + Sized> {
    width: usize,
    height: usize,
    pixels: Vec<P>
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
            pixels: vec
        }
    }

    /// Generate Raster from a function.
    pub fn generate<F: FnMut(usize, usize) -> P>(width: usize, height: usize, f: &mut F) -> Raster<P> {
        let mut vec = Vec::with_capacity(width * height);
        for y in 0 .. height {
            for x in 0 .. width {
                vec.push(f(x, y));
            }
        }
        Raster {
            width,
            height,
            pixels: vec
        }
    }
}

/// Something like Raster (object safe)
pub trait RasterishObj<P: Copy + Sized + Default> {
    /// Gets the region width.
    fn width(&self) -> usize;
    /// Gets the region height.
    fn height(&self) -> usize;
    /// Gets a row.
    fn row<'a>(&'a self, y: usize) -> &'a [P];

    /// Returns true if the given point is in-range.
    #[inline]
    fn in_range(&self, x: usize, y: usize) -> bool {
        x < self.width() && y < self.height()
    }

    /// Returns true if the given point is in-range.
    #[inline]
    fn in_range_signed(&self, x: isize, y: isize) -> bool {
        x >= 0 && y >= 0 && x < (self.width() as isize) && y < (self.height() as isize)
    }

    /// Gets a span.
    #[inline]
    fn span<'a>(&'a self, x: usize, y: usize, width: usize) -> &'a [P] {
        &self.row(y)[x .. x + width]
    }

    /// Gets a pixel.
    #[inline]
    fn pixel<'a>(&'a self, x: usize, y: usize) -> P {
        self.row(y)[x]
    }

    /// Creates a Raster from this Rasterish.
    fn to_raster(&self) -> Raster<P> {
        let mut res = Raster::new(self.width(), self.height());
        for y in 0..self.height() {
            (*res.row_mut(y)).copy_from_slice(self.row(y));
        }
        res
    }

    /// Outputs a not-mutable region.
    fn region<'x>(&'x self, x: usize, y: usize, width: usize, height: usize) -> RasterRegion<'x, P>;
}

/// Something like Raster (object safe)
pub trait RasterishMutObj<P: Copy + Sized + Default> : RasterishObj<P> {
    /// Gets a row mutably.
    fn row_mut<'a>(&'a mut self, y: usize) -> &'a mut [P];

    /// Gets a span mutably.
    #[inline]
    fn span_mut<'a>(&'a mut self, x: usize, y: usize, width: usize) -> &'a mut [P] {
        &mut self.row_mut(y)[x .. x + width]
    }

    /// Gets a pixel mutably.
    #[inline]
    fn pixel_mut<'a>(&'a mut self, x: usize, y: usize) -> &'a mut P {
        &mut self.row_mut(y)[x]
    }

    /// Outputs a mutable region.
    fn region_mut<'x>(&'x mut self, x: usize, y: usize, width: usize, height: usize) -> RasterRegionMut<'x, P>;
}

/// Something like Raster.
pub trait Rasterish<P: Copy + Sized + Default> : RasterishObj<P> {
    /// Remap the colours of this Rasterish in some way, creating a Raster.
    fn map<R: Copy + Sized + Default, F: FnMut(usize, usize, P) -> R>(&self, f: &mut F) -> Raster<R> {
        Raster::generate(self.width(), self.height(), &mut |x, y| {
            f(x, y, self.pixel(x, y))
        })
    }
}

pub struct RasterRegion<'a, P: Copy + Sized> {
    backing: &'a dyn RasterishObj<P>,
    x: usize,
    y: usize,
    width: usize,
    height: usize,
}

pub struct RasterRegionMut<'a, P: Copy + Sized> {
    backing: &'a mut dyn RasterishMutObj<P>,
    x: usize,
    y: usize,
    width: usize,
    height: usize,
}

/// Something like Raster.
pub trait RasterishMut<P: Copy + Sized + Default> : Rasterish<P> + RasterishMutObj<P> {
}

impl<P: Copy + Sized + Default> RasterishObj<P> for Raster<P> {
    #[inline]
    fn width(&self) -> usize { self.width }
    #[inline]
    fn height(&self) -> usize { self.height }
    #[inline]
    fn row<'a>(&'a self, y: usize) -> &'a [P] {
        let pos = y * self.width;
        &self.pixels[pos .. pos + self.width]
    }
    #[inline]
    fn region<'x>(&'x self, x: usize, y: usize, width: usize, height: usize) -> RasterRegion<'x, P> {
        RasterRegion {
            backing: self,
            x,
            y,
            width,
            height,
        }
    }
}

impl<P: Copy + Sized + Default> Rasterish<P> for Raster<P> {
}

impl<P: Copy + Sized + Default> RasterishMutObj<P> for Raster<P> {
    #[inline]
    fn row_mut<'a>(&'a mut self, y: usize) -> &'a mut [P] {
        let pos = y * self.width;
        &mut self.pixels[pos .. pos + self.width]
    }
    #[inline]
    fn region_mut<'x>(&'x mut self, x: usize, y: usize, width: usize, height: usize) -> RasterRegionMut<'x, P> {
        RasterRegionMut {
            backing: self,
            x,
            y,
            width,
            height,
        }
    }
}
impl<P: Copy + Sized + Default> RasterishMut<P> for Raster<P> {
}

impl<'b, P: Copy + Sized + Default> RasterishObj<P> for RasterRegion<'b, P> {
    #[inline]
    fn width(&self) -> usize { self.width }
    #[inline]
    fn height(&self) -> usize { self.height }
    #[inline]
    fn row<'a>(&'a self, y: usize) -> &'a [P] {
        &self.backing.row(self.y + y)[self.x .. self.x + self.width]
    }
    #[inline]
    fn region<'x>(&'x self, x: usize, y: usize, width: usize, height: usize) -> RasterRegion<'x, P> {
        RasterRegion {
            backing: self,
            x,
            y,
            width,
            height,
        }
    }
}

impl<'b, P: Copy + Sized + Default> Rasterish<P> for RasterRegion<'b, P> {
}

impl<'b, P: Copy + Sized + Default> RasterishObj<P> for RasterRegionMut<'b, P> {
    #[inline]
    fn width(&self) -> usize { self.width }
    #[inline]
    fn height(&self) -> usize { self.height }
    #[inline]
    fn row<'a>(&'a self, y: usize) -> &'a [P] {
        &self.backing.row(self.y + y)[self.x .. self.x + self.width]
    }
    #[inline]
    fn region<'x>(&'x self, x: usize, y: usize, width: usize, height: usize) -> RasterRegion<'x, P> {
        RasterRegion {
            backing: self,
            x,
            y,
            width,
            height,
        }
    }
}

impl<'b, P: Copy + Sized + Default> Rasterish<P> for RasterRegionMut<'b, P> {
}

impl<'b, P: Copy + Sized + Default> RasterishMutObj<P> for RasterRegionMut<'b, P> {
    #[inline]
    fn row_mut<'a>(&'a mut self, y: usize) -> &'a mut [P] {
        &mut self.backing.row_mut(self.y + y)[self.x .. self.x + self.width]
    }
    #[inline]
    fn region_mut<'x>(&'x mut self, x: usize, y: usize, width: usize, height: usize) -> RasterRegionMut<'x, P> {
        RasterRegionMut {
            backing: self,
            x,
            y,
            width,
            height,
        }
    }
}
impl<'b, P: Copy + Sized + Default> RasterishMut<P> for RasterRegionMut<'b, P> {
}
