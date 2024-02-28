//! Colour model.

use super::*;

/// ARGB32 value
#[derive(Copy, Clone, Eq, PartialEq, Ord, PartialOrd, Default)]
pub struct ARGB32(u8, u8, u8, u8);

impl ARGB32 {
    /// Separates channels into two rasters (for use with bitdither).
    pub fn separate_channels(source: &dyn RasterishObj<ARGB32>) -> (Raster<u8>, Raster<u8>, Raster<u8>, Raster<u8>) {
        (
            source.map(&mut |_, _, v| v.0),
            source.map(&mut |_, _, v| v.1),
            source.map(&mut |_, _, v| v.2),
            source.map(&mut |_, _, v| v.3)
        )
    }

    /// Combines channels (for use with bitdither).
    pub fn combine_channels(a: &dyn RasterishObj<u8>, r: &dyn RasterishObj<u8>, g: &dyn RasterishObj<u8>, b: &dyn RasterishObj<u8>) -> Raster<ARGB32> {
        Raster::generate(a.width(), a.height(), &mut |x, y| {
            ARGB32(a.pixel(x, y), r.pixel(x, y), g.pixel(x, y), b.pixel(x, y))
        })
    }
}

/// This represents a colour model.
/// The colour models are intended to be used via trait object to prevent over-genericization.
/// In addition, doing things this way allows for indexed-colour models to be represented.
pub trait ColourModel<P: Copy + Sized + Default> {
    /// Converts a pixel from this colour model to an ARGB32 colour.
    /// Invalid values should be handled by ignoring invalid bits or so forth.
    fn decode(&self, data: P) -> ARGB32;

    /// Decodes an entire raster.
    fn decode_raster(&self, source: &dyn RasterishObj<P>) -> Raster<ARGB32> {
        source.map(&mut |_, _, v| {
            self.decode(v)
        })
    }

    /// Converts a pixel to this colour model from an ARGB32 colour in a naive manner.
    /// This is always referred to as the "floor" dither method.
    /// This must be lossless for any value returned from `decode`, assuming no invalid values.
    /// The inverse is not true, `decode` may not return what was passed to `encode` (due to loss of precision in the format).
    fn encode(&self, v: ARGB32) -> P;
}

struct ColourModelARGB32();

impl ColourModel<ARGB32> for ColourModelARGB32 {
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
    // lengths & bitcopyfields
    r_bcf: BitCopyField,
    g_bcf: BitCopyField,
    b_bcf: BitCopyField,
    // masks
    r_mask: u16,
    g_mask: u16,
    b_mask: u16,
    // transparency nudge
    nudge: u16,
}

/// RGBxxx colour model
pub struct ColourModelRGB16SPR {
    config: RGB16BitPositions,
}

impl ColourModel<u16> for ColourModelRGB16SPR {
    fn decode(&self, data: u16) -> ARGB32 {
        // If RGB555 has the upper bit as 1, it's in something of a superposition as to if it's transparent.
        // What it very much isn't is "valid", but we can ignore that...
        // Decompose to RGB
        let mut r = ((data & self.config.r_mask) >> self.config.r_pos) as u8;
        let mut g = ((data & self.config.g_mask) >> self.config.g_pos) as u8;
        let mut b = ((data & self.config.b_mask) >> self.config.b_pos) as u8;
        if r == 0 && g == 0 && b == 0 {
            return ARGB32(0, 0, 0, 0);
        }
        // Bit-copy
        r = self.config.r_bcf.shiftup(r as usize) as u8;
        g = self.config.g_bcf.shiftup(g as usize) as u8;
        b = self.config.b_bcf.shiftup(b as usize) as u8;
        // Recompose
        ARGB32(255, r, g, b)
    }

    fn encode(&self, v: ARGB32) -> u16 {
        if v.0 < 128 {
            return 0;
        }
        let r = self.config.r_bcf.shiftdown(v.1 as usize) as u8;
        let g = self.config.g_bcf.shiftdown(v.2 as usize) as u8;
        let b = self.config.b_bcf.shiftdown(v.3 as usize) as u8;
        let test = ((r as u16) << self.config.r_pos)
            | ((g as u16) << self.config.g_pos)
            | ((b as u16) << self.config.b_pos);
        if test == 0 {
            return self.config.nudge;
        }
        test
    }
}

/// RGB555 colour model
pub struct ColourModelRGB16BLK {
    config: RGB16BitPositions,
}

impl ColourModel<u16> for ColourModelRGB16BLK {
    fn decode(&self, data: u16) -> ARGB32 {
        // Decompose to RGB
        let mut r = ((data & self.config.r_mask) >> self.config.r_pos) as u8;
        let mut g = ((data & self.config.g_mask) >> self.config.g_pos) as u8;
        let mut b = ((data & self.config.b_mask) >> self.config.b_pos) as u8;
        // Bit-copy
        r = self.config.r_bcf.shiftup(r as usize) as u8;
        g = self.config.g_bcf.shiftup(g as usize) as u8;
        b = self.config.b_bcf.shiftup(b as usize) as u8;
        // Recompose
        ARGB32(255, r, g, b)
    }

    fn encode(&self, v: ARGB32) -> u16 {
        let r = self.config.r_bcf.shiftdown(v.1 as usize) as u8;
        let g = self.config.g_bcf.shiftdown(v.2 as usize) as u8;
        let b = self.config.b_bcf.shiftdown(v.3 as usize) as u8;
        let test = ((r as u16) << self.config.r_pos)
            | ((g as u16) << self.config.g_pos)
            | ((b as u16) << self.config.b_pos);
        test
    }
}

/// RGB565 bit positions
const RGB565BP: RGB16BitPositions = RGB16BitPositions {
    r_pos: 11,
    g_pos: 5,
    b_pos: 0,
    r_bcf: BitCopyField::new(5, 8),
    g_bcf: BitCopyField::new(6, 8),
    b_bcf: BitCopyField::new(5, 8),
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
    r_bcf: BitCopyField::new(5, 8),
    g_bcf: BitCopyField::new(5, 8),
    b_bcf: BitCopyField::new(5, 8),
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
    r_bcf: BitCopyField::new(5, 8),
    g_bcf: BitCopyField::new(5, 8),
    b_bcf: BitCopyField::new(5, 8),
    r_mask: 0xF800,
    g_mask: 0x07C0,
    b_mask: 0x003E,
    nudge: 2,
};

/// Pair for sprite/BLK versions of 16-bit colour models.
pub struct StaticColourModelPair<P: Copy + Sized + Default + 'static> {
    pub spr: &'static dyn ColourModel<P>,
    pub blk: &'static dyn ColourModel<P>,
}

// -- The Macro --

/// This macro handles all the different dithering options/etc. for 16-bit images.
macro_rules! gen_cm16 {
    ($ty: ident, $id:ident) => {
        &$ty {
            config: $id
        }
    }
}
/// This macro wraps gen_cm16 for spr/blk
macro_rules! gen_cm16pair {
    ($id:ident) => {
        StaticColourModelPair {
            spr: gen_cm16!(ColourModelRGB16SPR, $id),
            blk: gen_cm16!(ColourModelRGB16BLK, $id)
        }
    }
}

// -- Actual Instances --

pub const CM_ARGB32: &'static dyn ColourModel<ARGB32> = &ColourModelARGB32();

/// RGB565LE, the regular colour model supported by regular C2E.
pub const CM_RGB565: StaticColourModelPair<u16> = gen_cm16pair!(RGB565BP);

/// RGB1555LE, a "low-spec" colour model supported by regular C2E.
pub const CM_RGB1555: StaticColourModelPair<u16> = gen_cm16pair!(RGB1555BP);

/// RGB5551BE, used for Mac-native C2E.
pub const CM_RGB5551: StaticColourModelPair<u16> = gen_cm16pair!(RGB5551BP);
