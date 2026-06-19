// libkc3ds - General-purpose Rust library for C3/DS
// Written starting in 2024 by contributors (see CREDITS.txt)
// To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
// You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

use super::super::io::{Endianness, ENDIANNESS_BE, ENDIANNESS_LE};
use super::*;
use std::fmt::Display;

/// Sprite file type.
#[derive(Copy, Clone, Debug)]
pub enum SprType {
    S16(S16Type),
    C16(C16Type),
    S32,
}

#[derive(Copy, Clone, Debug)]
pub enum S16Type {
    S16_565,
    S16_555,
    M16,
    N16,
}

#[derive(Copy, Clone, Debug)]
pub enum C16Type {
    C16_565,
    C16_555,
}

impl SprType {
    /// The magic number (little-endian 32-bit integer at start)
    pub fn magic(&self) -> u32 {
        match self {
            Self::S16(S16Type::S16_565) => 1,
            Self::S16(S16Type::S16_555) => 0,
            Self::S16(S16Type::N16) => 0x01000000,
            Self::S16(S16Type::M16) => 0x03000000,
            Self::C16(C16Type::C16_565) => 3,
            Self::C16(C16Type::C16_555) => 2,
            Self::S32 => 4,
        }
    }

    /// 32-bit magic number, when interpreted as little-endian
    pub fn of_magic(magic: u32) -> Option<Self> {
        match magic {
            1 => Some(Self::S16(S16Type::S16_565)),
            3 => Some(Self::C16(C16Type::C16_565)),
            0 => Some(Self::S16(S16Type::S16_555)),
            2 => Some(Self::C16(C16Type::C16_555)),
            0x01000000 => Some(Self::S16(S16Type::N16)),
            0x03000000 => Some(Self::S16(S16Type::M16)),
            4 => Some(Self::S32),
            _ => None,
        }
    }

    /// Is this a valid format for BLK?
    pub fn blk_capable(&self) -> bool {
        match self {
            Self::S16(_) => true,
            Self::C16(_) => false,
            Self::S32 => true,
        }
    }

    /// Are header fields etc. big-endian?
    pub fn is_be(&self) -> bool {
        match self {
            Self::S16(S16Type::S16_565) => false,
            Self::S16(S16Type::S16_555) => false,
            Self::C16(_) => false,
            Self::S16(S16Type::M16) => true,
            Self::S16(S16Type::N16) => true,
            Self::S32 => false,
        }
    }

    /// Endianness as a dynamic trait object
    pub fn endianness(&self) -> &'static dyn Endianness {
        if self.is_be() {
            &ENDIANNESS_BE
        } else {
            &ENDIANNESS_LE
        }
    }

    /// Colour model.
    pub fn to_cm(&self) -> &'static dyn ColourModel {
        match self {
            Self::S16(S16Type::S16_565) => &CM_RGB565,
            Self::S16(S16Type::S16_555) => &CM_RGB1555,
            Self::C16(C16Type::C16_565) => &CM_RGB565,
            Self::C16(C16Type::C16_555) => &CM_RGB1555,
            Self::S16(S16Type::M16) => &CM_RGB5551,
            Self::S16(S16Type::N16) => &CM_RGB5551,
            Self::S32 => &CM_ARGB32,
        }
    }

    ///
    pub fn describe(&self) -> &'static str {
        match self {
            Self::S16(S16Type::S16_565) => "S16 RGB565 LE",
            Self::S16(S16Type::S16_555) => "S16 RGB555 LE",
            Self::C16(C16Type::C16_565) => "C16 RGB565 LE",
            Self::C16(C16Type::C16_555) => "C16 RGB555 LE",
            Self::S16(S16Type::M16) => "M16 RGB5551 BE",
            Self::S16(S16Type::N16) => "N16 RGB5551 BE",
            Self::S32 => "S32 PNG LE",
        }
    }

    /// Translates C16 to equivalent S16 or leaves alone.
    pub fn to_equivalent_s16(&self) -> SprType {
        match self {
            Self::C16(C16Type::C16_565) => Self::S16(S16Type::S16_565),
            Self::C16(C16Type::C16_555) => Self::S16(S16Type::S16_555),
            _ => *self,
        }
    }

    /// Translates to an equivalent C16 type.
    pub fn to_equivalent_c16(&self) -> Option<C16Type> {
        match self {
            Self::S16(S16Type::S16_565) => Some(C16Type::C16_565),
            Self::S16(S16Type::S16_555) => Some(C16Type::C16_555),
            _ => None,
        }
    }
}

impl Display for SprType {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.write_str(self.describe())
    }
}

/// Frame in a sprite file.
pub type SprFrame = Raster<Pixel>;

/// Decompressed C16 or unwrapped S16 sheet.
#[derive(Clone)]
pub struct SprSheet {
    /// CS16 type identifier.
    pub id: SprType,
    /// Each individual frame.
    pub frames: Vec<SprFrame>,
}
