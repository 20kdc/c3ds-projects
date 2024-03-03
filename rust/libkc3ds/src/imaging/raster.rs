// libkc3ds - General-purpose Rust library for C3/DS
// Written starting in 2024 by contributors (see CREDITS.txt)
// To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
// You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

use super::Raster;

/// Covers [Raster], [super::RasterTile], and [RasterRegion].
pub trait RasterishObj<P: Copy + Sized + Default> {
    /// Gets the region width.
    fn width(&self) -> usize;
    /// Gets the region height.
    fn height(&self) -> usize;
    /// Gets a row.
    fn row(&self, y: usize) -> &[P];

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
    fn span(&self, x: usize, y: usize, width: usize) -> &[P] {
        &self.row(y)[x..x + width]
    }

    /// Gets a pixel.
    #[inline]
    fn pixel(&self, x: usize, y: usize) -> P {
        self.row(y)[x]
    }

    /// Gets a pixel (coordinates checked).
    #[inline]
    fn pixel_checked_signed(&self, x: isize, y: isize) -> Option<P> {
        if !self.in_range_signed(x, y) {
            None
        } else {
            Some(self.pixel(x as usize, y as usize))
        }
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
    fn region(&self, x: usize, y: usize, width: usize, height: usize) -> RasterRegion<'_, P>;

    /// Outputs a clipped region.
    /// Because X/Y are not signed, there is never any desync in the coordinate system for non-empty regions.
    fn region_clipped(
        &self,
        x: usize,
        y: usize,
        width: usize,
        height: usize,
    ) -> RasterRegion<'_, P> {
        if x >= self.width() || y >= self.height() {
            self.region(0, 0, 0, 0)
        } else {
            let remaining_w = self.width() - x;
            let remaining_h = self.height() - y;
            self.region(
                x,
                y,
                if width > remaining_w {
                    remaining_w
                } else {
                    width
                },
                if height > remaining_h {
                    remaining_h
                } else {
                    height
                },
            )
        }
    }
}

/// Something like Raster (object safe)
pub trait RasterishMutObj<P: Copy + Sized + Default>: RasterishObj<P> {
    /// Gets a row mutably.
    fn row_mut(&mut self, y: usize) -> &mut [P];

    /// Gets a span mutably.
    #[inline]
    fn span_mut(&mut self, x: usize, y: usize, width: usize) -> &mut [P] {
        &mut self.row_mut(y)[x..x + width]
    }

    /// Gets a pixel mutably.
    #[inline]
    fn pixel_mut(&mut self, x: usize, y: usize) -> &mut P {
        &mut self.row_mut(y)[x]
    }

    /// Sets a pixel.
    #[inline]
    fn set_pixel(&mut self, x: usize, y: usize, v: P) {
        *self.pixel_mut(x, y) = v;
    }

    /// Outputs a mutable region.
    fn region_mut(
        &mut self,
        x: usize,
        y: usize,
        width: usize,
        height: usize,
    ) -> RasterRegionMut<'_, P>;

    /// Outputs a clipped mutable region.
    /// Because X/Y are not signed, there is never any desync in the coordinate system for non-empty regions.
    fn region_clipped_mut(
        &mut self,
        x: usize,
        y: usize,
        width: usize,
        height: usize,
    ) -> RasterRegionMut<'_, P> {
        if x >= self.width() || y >= self.height() {
            self.region_mut(0, 0, 0, 0)
        } else {
            let remaining_w = self.width() - x;
            let remaining_h = self.height() - y;
            self.region_mut(
                x,
                y,
                if width > remaining_w {
                    remaining_w
                } else {
                    width
                },
                if height > remaining_h {
                    remaining_h
                } else {
                    height
                },
            )
        }
    }
}

pub struct RasterRegion<'a, P: Copy + Sized> {
    pub backing: &'a dyn RasterishObj<P>,
    pub x: usize,
    pub y: usize,
    pub width: usize,
    pub height: usize,
}

pub struct RasterRegionMut<'a, P: Copy + Sized> {
    pub backing: &'a mut dyn RasterishMutObj<P>,
    pub x: usize,
    pub y: usize,
    pub width: usize,
    pub height: usize,
}

/// Something like Raster.
pub trait Rasterish<P: Copy + Sized + Default>: RasterishObj<P> {
    /// Remap the colours of this Rasterish in some way, creating a Raster.
    fn map<R: Copy + Sized + Default, F: FnMut(usize, usize, P) -> R>(
        &self,
        f: &mut F,
    ) -> Raster<R> {
        Raster::generate(self.width(), self.height(), &mut |x, y| {
            f(x, y, self.pixel(x, y))
        })
    }
}

/// Something like Raster.
pub trait RasterishMut<P: Copy + Sized + Default>: Rasterish<P> + RasterishMutObj<P> {
    /// Remap the colours of this Rasterish in some way, in-place.
    fn map_inplace<F: FnMut(usize, usize, P) -> P>(&mut self, f: &mut F) {
        for y in 0..self.height() {
            let row = self.row_mut(y);
            for (x, v) in row.iter_mut().enumerate() {
                *v = f(x, y, *v);
            }
        }
    }

    /// Draw the source to the given coordinates using the given "applier" function `(src, dst) -> res`.
    /// This is NOT checked to ensure in-bounds (will panic) - see [draw_clipped].
    fn draw<S: Rasterish<P>, F: Fn(P, P) -> P>(
        &mut self,
        source: &S,
        x: usize,
        y: usize,
        blit: &F,
    ) {
        for sy in 0..source.height() {
            let src_row = source.row(sy);
            let dst_row = self.row_mut(sy + y);
            for (sx, sv) in src_row.iter().enumerate() {
                dst_row[x + sx] = blit(*sv, dst_row[x + sx]);
            }
        }
    }

    /// Blit the source to the given coordinates using the given "applier" function `(src, dst) -> res`.
    /// This is checked and won't draw out-of-bounds.
    #[inline]
    fn draw_clipped<S: Rasterish<P>, F: Fn(P, P) -> P>(
        &mut self,
        source: &S,
        x: usize,
        y: usize,
        blit: &F,
    ) {
        let mut region = self.region_clipped_mut(x, y, source.width(), source.height());
        region.draw(
            &source.region_clipped(0, 0, region.width(), region.height()),
            0,
            0,
            blit,
        );
    }

    /// Copy the source to the given coordinates.
    /// This is NOT checked to ensure in-bounds (will panic) - see [copy_clipped].
    fn copy<S: Rasterish<P>>(&mut self, source: &S, x: usize, y: usize) {
        for sy in 0..source.height() {
            let src_row = source.row(sy);
            let dst_row = &mut self.row_mut(sy + y)[x..x + source.width()];
            dst_row.copy_from_slice(src_row);
        }
    }

    /// Copy the source to the given coordinates.
    /// This is checked and won't draw out-of-bounds.
    #[inline]
    fn copy_clipped<S: Rasterish<P>>(&mut self, source: &S, x: usize, y: usize) {
        let mut region = self.region_clipped_mut(x, y, source.width(), source.height());
        region.copy(
            &source.region_clipped(0, 0, region.width(), region.height()),
            0,
            0,
        );
    }
}

impl<'b, P: Copy + Sized + Default> RasterishObj<P> for RasterRegion<'b, P> {
    #[inline]
    fn width(&self) -> usize {
        self.width
    }
    #[inline]
    fn height(&self) -> usize {
        self.height
    }
    #[inline]
    fn row(&self, y: usize) -> &[P] {
        &self.backing.row(self.y + y)[self.x..self.x + self.width]
    }
    #[inline]
    fn region(&self, x: usize, y: usize, width: usize, height: usize) -> RasterRegion<'_, P> {
        RasterRegion {
            backing: self,
            x,
            y,
            width,
            height,
        }
    }
}

impl<'b, P: Copy + Sized + Default> RasterishObj<P> for RasterRegionMut<'b, P> {
    #[inline]
    fn width(&self) -> usize {
        self.width
    }
    #[inline]
    fn height(&self) -> usize {
        self.height
    }
    #[inline]
    fn row(&self, y: usize) -> &[P] {
        &self.backing.row(self.y + y)[self.x..self.x + self.width]
    }
    #[inline]
    fn region(&self, x: usize, y: usize, width: usize, height: usize) -> RasterRegion<'_, P> {
        RasterRegion {
            backing: self,
            x,
            y,
            width,
            height,
        }
    }
}

impl<'b, P: Copy + Sized + Default> RasterishMutObj<P> for RasterRegionMut<'b, P> {
    #[inline]
    fn row_mut(&mut self, y: usize) -> &mut [P] {
        &mut self.backing.row_mut(self.y + y)[self.x..self.x + self.width]
    }
    #[inline]
    fn region_mut(
        &mut self,
        x: usize,
        y: usize,
        width: usize,
        height: usize,
    ) -> RasterRegionMut<'_, P> {
        RasterRegionMut {
            backing: self,
            x,
            y,
            width,
            height,
        }
    }
}

// implement extensions

impl<'b, P: Copy + Sized + Default> Rasterish<P> for RasterRegion<'b, P> {}

impl<'b, P: Copy + Sized + Default> Rasterish<P> for RasterRegionMut<'b, P> {}

impl<'b, P: Copy + Sized + Default> RasterishMut<P> for RasterRegionMut<'b, P> {}

impl<P: Copy + Sized + Default> Rasterish<P> for dyn RasterishObj<P> + '_ {}

impl<P: Copy + Sized + Default> Rasterish<P> for dyn RasterishMutObj<P> + '_ {}

impl<P: Copy + Sized + Default> RasterishMut<P> for dyn RasterishMutObj<P> + '_ {}
