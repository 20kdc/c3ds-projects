// libkc3ds - General-purpose Rust library for C3/DS
// Written starting in 2024 by contributors (see CREDITS.txt)
// To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
// You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

use super::*;

pub trait CS16TypeCommon {
    /// 32-bit magic number, when interpreted as little-endian
    fn magic(&self) -> u32;

    /// Endianness.
    fn is_be(&self) -> bool;

    /// Looks up the actual colour model.
    fn to_cm16(&self) -> &'static ColourModelRGB16;
}

/// File type.
#[derive(Copy, Clone)]
pub enum CS16Type {
    S16(S16Type),
    C16(C16Type),
}

impl CS16Type {
    /// 32-bit magic number, when interpreted as little-endian
    pub fn of_magic(magic: u32) -> Option<CS16Type> {
        match magic {
            1 => Some(Self::S16(S16Type::S16_565)),
            3 => Some(Self::C16(C16Type::C16_565)),
            0 => Some(Self::S16(S16Type::S16_555)),
            2 => Some(Self::C16(C16Type::C16_555)),
            0x01000000 => Some(Self::S16(S16Type::M16)),
            0x03000000 => Some(Self::S16(S16Type::N16)),
            _ => None,
        }
    }
}

impl CS16TypeCommon for CS16Type {
    fn magic(&self) -> u32 {
        match self {
            Self::S16(s16) => s16.magic(),
            Self::C16(c16) => c16.magic(),
        }
    }

    fn is_be(&self) -> bool {
        match self {
            Self::S16(s16) => s16.is_be(),
            Self::C16(c16) => c16.is_be(),
        }
    }

    fn to_cm16(&self) -> &'static ColourModelRGB16 {
        match self {
            Self::S16(s16) => s16.to_cm16(),
            Self::C16(c16) => c16.to_cm16(),
        }
    }
}

/// Frame in an S16 file.
pub type S16Frame = Raster<u16>;

/// Decompressed C16 or unwrapped S16 sheet.
pub struct CS16Sheet {
    /// CS16 type identifier.
    pub id: CS16Type,
    /// Each individual frame.
    pub frames: Vec<S16Frame>,
}

#[derive(Copy, Clone)]
pub enum S16Type {
    /// s16 high-quality
    S16_565,
    /// s16 low-quality
    S16_555,
    /// m16
    M16,
    /// n16
    N16,
}

impl CS16TypeCommon for S16Type {
    fn magic(&self) -> u32 {
        match self {
            Self::S16_565 => 1,
            Self::S16_555 => 0,
            Self::M16 => 0x01000000,
            Self::N16 => 0x03000000,
        }
    }

    fn is_be(&self) -> bool {
        match self {
            Self::S16_565 => false,
            Self::S16_555 => false,
            Self::M16 => true,
            Self::N16 => true,
        }
    }

    fn to_cm16(&self) -> &'static ColourModelRGB16 {
        match self {
            Self::S16_565 => &CM_RGB565,
            Self::S16_555 => &CM_RGB1555,
            Self::M16 => &CM_RGB5551,
            Self::N16 => &CM_RGB5551,
        }
    }
}

/// S16 sheet.
pub struct S16Sheet {
    /// S16 type identifier.
    pub id: S16Type,
    /// Each individual frame.
    pub frames: Vec<S16Frame>,
}

#[derive(Copy, Clone)]
pub enum C16Type {
    C16_565,
    C16_555,
}

impl CS16TypeCommon for C16Type {
    fn magic(&self) -> u32 {
        match self {
            Self::C16_565 => 3,
            Self::C16_555 => 2,
        }
    }

    fn is_be(&self) -> bool {
        false
    }

    fn to_cm16(&self) -> &'static ColourModelRGB16 {
        match self {
            Self::C16_565 => &CM_RGB565,
            Self::C16_555 => &CM_RGB1555,
        }
    }
}

/// C16 sheet.
/// This is the compressed version.
pub struct C16Sheet {
    pub id: C16Type,
    pub frames: Vec<C16Frame>,
}

impl C16Sheet {
    /// Get the encoded size of this sheet.
    pub fn size(&self) -> usize {
        let mut total: usize = 6;
        for v in &self.frames {
            total += 8;
            total += 4 * (v.rows.len() - 1);
            total += v.size();
        }
        total
    }
}

/// A row, not including end-of-row markers or the end-of-image marker
pub type C16Row = Vec<u16>;

/// Single frame of a C16 file.
pub struct C16Frame {
    pub width: usize,
    pub rows: Vec<C16Row>,
}

impl C16Frame {
    /// Get the encoded size of this frame (without headers).
    pub fn size(&self) -> usize {
        let mut total: usize = 2; // end of image marker
        for v in &self.rows {
            total += (v.len() * 2) + 2;
        }
        total
    }
}
