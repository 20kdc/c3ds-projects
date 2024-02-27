//! Colour model.

use super::{Raster, RasterishObj, RasterishMutObj};
use std::collections::HashMap;

#[derive(Copy, Clone)]
pub struct ARGB32(u8, u8, u8, u8);

/// Method of dithering from ARGB32 rasters.
pub trait DitherMethod<P: Copy + Sized> {
    /// Name of the dithering method.
    fn name(&self) -> &str;
    /// Dithers a Rasterish to a target.
    fn dither(&self, source: &dyn RasterishObj<ARGB32>, target: &mut dyn RasterishMutObj<P>);
}

/// This represents a colour model.
/// The colour models are intended to be used via trait object to prevent over-genericization.
/// In addition, doing things this way allows for indexed-colour models to be represented.
pub trait ColourModel<P: Copy + Sized> {
    /// Dither methods for this colour model.
    fn visit_dither_methods(&self) -> HashMap<String, Box<dyn DitherMethod<P>>>;

    /// Converts a pixel from this colour model to an ARGB32 colour.
    /// Invalid values should be handled by ignoring invalid bits or so forth.
    fn decode(&self, data: P) -> ARGB32;

    /// Converts a pixel to this colour model from an ARGB32 colour in a naive manner.
    /// This must be lossless for any value returned from `decode`, assuming no invalid values.
    /// The inverse is not true, `decode` may not return what was passed to `encode` (due to loss of precision in the format).
    fn encode(&self, v: ARGB32) -> P;
}

pub struct ColourModelARGB32();

impl ColourModel<ARGB32> for ColourModelARGB32 {
    fn visit_dither_methods(&self) -> HashMap<String, Box<dyn DitherMethod<ARGB32>>> {
        HashMap::new()
    }

    fn decode(&self, data: ARGB32) -> ARGB32 {
        data
    }

    fn encode(&self, v: ARGB32) -> ARGB32 {
        v
    }
}

/// Bit positions for packed RGB
pub struct RGB16BitPositions {
    r_pos: u8,
    g_pos: u8,
    b_pos: u8,
    // lengths
    r_len: u8,
    g_len: u8,
    b_len: u8,
    // decode shl
    r_decshl: u8,
    g_decshl: u8,
    b_decshl: u8,
    // masks
    r_mask: u16,
    g_mask: u16,
    b_mask: u16,
    // transparency nudge
    nudge: u16,
}

/// RGB555 colour model
pub struct ColourModelRGB16<const ALPHA: bool> {
    config: RGB16BitPositions,
}

impl<const ALPHA: bool> ColourModel<u16> for ColourModelRGB16<ALPHA> {
    fn visit_dither_methods(&self) -> HashMap<String, Box<dyn DitherMethod<u16>>> {
        HashMap::new()
    }

    fn decode(&self, data: u16) -> ARGB32 {
        // If RGB555 has the upper bit as 1, it's in something of a superposition as to if it's transparent.
        // What it very much isn't is "valid", but we can ignore that...
        // Decompose to RGB
        let mut r = ((data & self.config.r_mask) >> self.config.r_pos) as u8;
        let mut g = ((data & self.config.g_mask) >> self.config.g_pos) as u8;
        let mut b = ((data & self.config.b_mask) >> self.config.b_pos) as u8;
        if r == 0 && g == 0 && b == 0 && ALPHA {
            return ARGB32(0, 0, 0, 0);
        }
        // Bit-copy
        r <<= self.config.r_decshl;
        r |= r >> self.config.r_len;
        g <<= self.config.g_decshl;
        g |= g >> self.config.g_len;
        b <<= self.config.b_decshl;
        b |= b >> self.config.b_len;
        // Recompose
        ARGB32(255, r, g, b)
    }

    fn encode(&self, v: ARGB32) -> u16 {
        if ALPHA && v.0 < 128 {
            return 0;
        }
        let mut r = v.1;
        let mut g = v.2;
        let mut b = v.3;
        r >>= self.config.r_decshl;
        g >>= self.config.g_decshl;
        b >>= self.config.b_decshl;
        let test = ((r as u16) << self.config.r_pos)
            | ((g as u16) << self.config.g_pos)
            | ((b as u16) << self.config.b_pos);
        if test == 0 && ALPHA {
            return self.config.nudge;
        }
        test
    }
}

/// RGB565 bit positions
const RGB565BP: RGB16BitPositions = RGB16BitPositions {
    r_pos: 11,
    g_pos: 5,
    b_pos: 0,
    r_len: 5,
    g_len: 6,
    b_len: 5,
    r_decshl: 3,
    g_decshl: 2,
    b_decshl: 3,
    r_mask: 0xF800,
    g_mask: 0x07E0,
    b_mask: 0x001F,
    nudge: 1,
};

/// RGB1555 bit positions
const RGB1555BP: RGB16BitPositions = RGB16BitPositions {
    r_pos: 10,
    g_pos: 5,
    b_pos: 0,
    r_len: 5,
    g_len: 5,
    b_len: 5,
    r_decshl: 3,
    g_decshl: 3,
    b_decshl: 3,
    r_mask: 0x7C00,
    g_mask: 0x03E0,
    b_mask: 0x001F,
    nudge: 1,
};

/// RGB5551 bit positions
const RGB5551BP: RGB16BitPositions = RGB16BitPositions {
    r_pos: 11,
    g_pos: 6,
    b_pos: 1,
    r_len: 5,
    g_len: 5,
    b_len: 5,
    r_decshl: 3,
    g_decshl: 3,
    b_decshl: 3,
    r_mask: 0xF800,
    g_mask: 0x07C0,
    b_mask: 0x003E,
    nudge: 2,
};

// -- Actual Instances --

/// RGB565LE, the regular colour model supported by regular C2E.
pub const RGB565_INSTANCE: ColourModelRGB16<true> = ColourModelRGB16 { config: RGB565BP };
/// RGB1555LE, a "low-spec" colour model supported by regular C2E.
pub const RGB1555_INSTANCE: ColourModelRGB16<true> = ColourModelRGB16 { config: RGB1555BP };
/// RGB5551BE, used for Mac-native C2E.
pub const RGB5551_INSTANCE: ColourModelRGB16<true> = ColourModelRGB16 { config: RGB5551BP };

/// RGB565LE, the regular colour model supported by regular C2E. BLK version, no alpha.
pub const RGB565BLK_INSTANCE: ColourModelRGB16<false> = ColourModelRGB16 { config: RGB565BP };
/// RGB1555LE, a "low-spec" colour model supported by regular C2E. BLK version, no alpha.
pub const RGB1555BLK_INSTANCE: ColourModelRGB16<false> = ColourModelRGB16 { config: RGB1555BP };
/// RGB5551BE, used for Mac-native C2E. BLK version, no alpha.
pub const RGB5551BLK_INSTANCE: ColourModelRGB16<false> = ColourModelRGB16 { config: RGB5551BP };
