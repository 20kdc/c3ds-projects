//! Colour model.

use super::*;

/// ARGB32 value
#[derive(Copy, Clone, Eq, PartialEq, Ord, PartialOrd, Default)]
pub struct ARGB32(pub u8, pub RGB24);

impl ARGB32 {
    /// Separates A channel to a separate raster
    pub fn raster_a(source: &dyn RasterishObj<ARGB32>) -> Raster<u8> {
        source.map(&mut |_, _, v| v.0)
    }

    /// Separates RGB channels to a separate raster
    pub fn raster_rgb(source: &dyn RasterishObj<ARGB32>) -> Raster<RGB24> {
        source.map(&mut |_, _, v| v.1)
    }

    /// Combines channels
    pub fn combine_channels(a: &dyn RasterishObj<u8>, rgb: &dyn RasterishObj<RGB24>) -> Raster<ARGB32> {
        Raster::generate(a.width(), a.height(), &mut |x, y| {
            ARGB32(a.pixel(x, y), rgb.pixel(x, y))
        })
    }

    /// Adds opaque alpha.
    pub fn opaque(rgb: &dyn RasterishObj<RGB24>) -> Raster<ARGB32> {
        rgb.map(&mut |_, _, v| {
            ARGB32(255, v)
        })
    }
}

/// RGB24 value
#[derive(Copy, Clone, Eq, PartialEq, Ord, PartialOrd, Default)]
pub struct RGB24(pub u8, pub u8, pub u8);

impl RGB24 {
    /// Separates channels into three rasters (for use with bitdither).
    pub fn separate_channels(source: &dyn RasterishObj<RGB24>) -> (Raster<u8>, Raster<u8>, Raster<u8>) {
        (
            source.map(&mut |_, _, v| v.0),
            source.map(&mut |_, _, v| v.1),
            source.map(&mut |_, _, v| v.2)
        )
    }

    /// Combines channels (for use with bitdither).
    pub fn combine_channels(r: &dyn RasterishObj<u8>, g: &dyn RasterishObj<u8>, b: &dyn RasterishObj<u8>) -> Raster<RGB24> {
        Raster::generate(r.width(), r.height(), &mut |x, y| {
            RGB24(r.pixel(x, y), g.pixel(x, y), b.pixel(x, y))
        })
    }
}

/// This represents a colour model.
/// The colour models are intended to be used via trait object to prevent over-genericization.
/// In addition, doing things this way allows for indexed-colour models to be represented.
pub trait ColourModel<P: Copy + Sized + Default> {
    /// Returns true if the given pixel would be opaque in a sprite context.
    fn opaque(&self, data: P) -> bool;

    /// Converts a pixel from this colour model to a RGB24 colour.
    /// Invalid values should be handled by ignoring invalid bits or so forth.
    fn decode(&self, data: P) -> RGB24;

    /// Decodes an entire raster.
    fn decode_raster_blk(&self, source: &dyn RasterishObj<P>) -> Raster<RGB24> {
        source.map(&mut |_, _, v| {
            self.decode(v)
        })
    }

    /// Decodes a raster with transparency handling.
    fn decode_raster_spr(&self, source: &dyn RasterishObj<P>) -> Raster<ARGB32> {
        source.map(&mut |_, _, v| {
            ARGB32(if self.opaque(v) { 255 } else { 0 }, self.decode(v))
        })
    }

    /// Converts a pixel to this colour model from a RGB24 colour in a naive manner.
    /// This is always referred to as the "floor" dither method.
    /// This must be lossless for any value returned from `decode`, assuming no invalid values.
    /// The inverse is not true, `decode` may not return what was passed to `encode` (due to loss of precision in the format).
    fn encode(&self, v: RGB24) -> P;

    /// Boxify self - essentially a dynamic copy
    fn to_box(&self) -> Box<&dyn ColourModel<P>>;
}

/// Bit positions for packed RGB
#[derive(Clone, Copy)]
pub struct ColourModelRGB16 {
    pub r_pos: u8,
    pub g_pos: u8,
    pub b_pos: u8,
    // lengths & bitcopyfields
    pub r_bits: BitCopyField,
    pub g_bits: BitCopyField,
    pub b_bits: BitCopyField,
    // masks
    pub r_mask: u16,
    pub g_mask: u16,
    pub b_mask: u16,
    // transparency nudge
    pub nudge: u16,
    // "if none of these bits are 1, this is transparent" mask
    pub transparency_check_mask: u16,
}

impl ColourModel<u16> for ColourModelRGB16 {
    #[inline]
    fn opaque(&self, data: u16) -> bool {
        (self.transparency_check_mask & data) != 0
    }

    #[inline]
    fn decode(&self, data: u16) -> RGB24 {
        // Decompose to RGB
        let mut r = ((data & self.r_mask) >> self.r_pos) as u8;
        let mut g = ((data & self.g_mask) >> self.g_pos) as u8;
        let mut b = ((data & self.b_mask) >> self.b_pos) as u8;
        // Bit-copy
        r = self.r_bits.shiftup(r as usize) as u8;
        g = self.g_bits.shiftup(g as usize) as u8;
        b = self.b_bits.shiftup(b as usize) as u8;
        // Recompose
        RGB24(r, g, b)
    }

    #[inline]
    fn encode(&self, v: RGB24) -> u16 {
        let r = self.r_bits.shiftdown(v.0 as usize) as u8;
        let g = self.g_bits.shiftdown(v.1 as usize) as u8;
        let b = self.b_bits.shiftdown(v.2 as usize) as u8;
        let test = ((r as u16) << self.r_pos)
            | ((g as u16) << self.g_pos)
            | ((b as u16) << self.b_pos);
        test
    }

    fn to_box(&self) -> Box<&dyn ColourModel<u16>> {
        Box::new(self)
    }
}

/// RGB565 bit positions
pub const CM_RGB565BP: ColourModelRGB16 = ColourModelRGB16 {
    r_pos: 11,
    g_pos: 5,
    b_pos: 0,
    r_bits: BitCopyField::new(5, 8),
    g_bits: BitCopyField::new(6, 8),
    b_bits: BitCopyField::new(5, 8),
    r_mask: 0xF800,
    g_mask: 0x07E0,
    b_mask: 0x001F,
    nudge:  0x0020,
    transparency_check_mask: 0xFFFF
};

/// RGB1555 bit positions
pub const CM_RGB1555BP: ColourModelRGB16 = ColourModelRGB16 {
    r_pos: 10,
    g_pos: 5,
    b_pos: 0,
    r_bits: BitCopyField::new(5, 8),
    g_bits: BitCopyField::new(5, 8),
    b_bits: BitCopyField::new(5, 8),
    r_mask: 0x7C00,
    g_mask: 0x03E0,
    b_mask: 0x001F,
    nudge: 1,
    transparency_check_mask: 0x7FFF
};

/// RGB5551 bit positions
pub const CM_RGB5551BP: ColourModelRGB16 = ColourModelRGB16 {
    r_pos: 11,
    g_pos: 6,
    b_pos: 1,
    r_bits: BitCopyField::new(5, 8),
    g_bits: BitCopyField::new(5, 8),
    b_bits: BitCopyField::new(5, 8),
    r_mask: 0xF800,
    g_mask: 0x07C0,
    b_mask: 0x003E,
    nudge: 2,
    transparency_check_mask: 0xFFFE
};
