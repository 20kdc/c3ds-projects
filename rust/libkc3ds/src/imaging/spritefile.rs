// libkc3ds - General-purpose Rust library for C3/DS
// Written starting in 2024 by contributors (see CREDITS.txt)
// To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
// You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

use super::super::io::{Endianness, ENDIANNESS_BE, ENDIANNESS_LE};
use super::*;
use std::fmt::Display;

pub trait CS16TypeCommon {
    /// 32-bit magic number, when interpreted as little-endian
    fn magic(&self) -> u32;

    /// Describes the type.
    fn describe(&self) -> &'static str;

    /// Endianness.
    fn is_be(&self) -> bool;

    /// Endianness.
    fn endianness(&self) -> &'static dyn Endianness {
        if self.is_be() {
            &ENDIANNESS_BE
        } else {
            &ENDIANNESS_LE
        }
    }

    /// Looks up the actual colour model.
    fn to_cm16(&self) -> &'static ColourModelRGB16;

    /// Translates this C16Type to the equivalent S16Type.
    fn to_equivalent_s16(&self) -> S16Type;
}

impl Display for CS16Type {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.write_str(self.describe())
    }
}

/// File type.
#[derive(Copy, Clone, Debug)]
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
            0x01000000 => Some(Self::S16(S16Type::N16)),
            0x03000000 => Some(Self::S16(S16Type::M16)),
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

    fn describe(&self) -> &'static str {
        match self {
            Self::S16(s16) => s16.describe(),
            Self::C16(c16) => c16.describe(),
        }
    }

    fn to_equivalent_s16(&self) -> S16Type {
        match self {
            Self::S16(s16) => s16.to_equivalent_s16(),
            Self::C16(c16) => c16.to_equivalent_s16(),
        }
    }
}

/// Frame in an S16 file.
pub type S16Frame = Raster<u16>;

/// Decompressed C16 or unwrapped S16 sheet.
/// Has [From] interaction to/from [S16Sheet].
/// Can also be converted from [C16Sheet] (decompresses) but not the inverse (convert to S16Sheet and compress).
#[derive(Clone)]
pub struct CS16Sheet {
    /// CS16 type identifier.
    pub id: CS16Type,
    /// Each individual frame.
    pub frames: Vec<S16Frame>,
}

impl From<S16Sheet> for CS16Sheet {
    fn from(value: S16Sheet) -> Self {
        CS16Sheet {
            id: CS16Type::S16(value.id),
            frames: value.frames,
        }
    }
}

impl From<C16Sheet> for CS16Sheet {
    fn from(value: C16Sheet) -> Self {
        CS16Sheet {
            id: CS16Type::C16(value.id),
            frames: value.frames.iter().map(|v| v.decompress()).collect(),
        }
    }
}

#[derive(Copy, Clone, Debug)]
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
            Self::N16 => 0x01000000,
            Self::M16 => 0x03000000,
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

    fn describe(&self) -> &'static str {
        match self {
            Self::S16_565 => "S16 RGB565 LE",
            Self::S16_555 => "S16 RGB555 LE",
            Self::M16 => "M16 RGB5551 BE",
            Self::N16 => "N16 RGB5551 BE",
        }
    }

    fn to_equivalent_s16(&self) -> S16Type {
        *self
    }
}

impl S16Type {
    /// Translates this S16Type to the equivalent C16Type.
    pub fn to_equivalent_c16(&self) -> Option<C16Type> {
        match self {
            Self::S16_565 => Some(C16Type::C16_565),
            Self::S16_555 => Some(C16Type::C16_555),
            _ => None
        }
    }
}

/// S16 sheet.
#[derive(Clone)]
pub struct S16Sheet {
    /// S16 type identifier.
    pub id: S16Type,
    /// Each individual frame.
    pub frames: Vec<S16Frame>,
}

impl S16Sheet {
    /// Compresses. Will return None if this is in a format which cannot be compressed (N/M16).
    pub fn compress(&self) -> Option<C16Sheet> {
        self.id.to_equivalent_c16().map(|v| {
            C16Sheet {
                id: v,
                frames: self.frames.iter().map(|f| {
                    C16Frame::compress(f)
                }).collect()
            }
        })
    }
}

impl From<CS16Sheet> for S16Sheet {
    fn from(value: CS16Sheet) -> Self {
        S16Sheet {
            id: value.id.to_equivalent_s16(),
            frames: value.frames,
        }
    }
}

#[derive(Copy, Clone, Debug)]
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

    fn describe(&self) -> &'static str {
        match self {
            Self::C16_565 => "C16 RGB565 LE",
            Self::C16_555 => "C16 RGB555 LE",
        }
    }

    fn to_equivalent_s16(&self) -> S16Type {
        match self {
            Self::C16_565 => S16Type::S16_565,
            Self::C16_555 => S16Type::S16_555,
        }
    }
}

/// C16 sheet.
/// This is the compressed version.
#[derive(Clone)]
pub struct C16Sheet {
    pub id: C16Type,
    pub frames: Vec<C16Frame>,
}

/// A row, not including end-of-row markers or the end-of-image marker
pub type C16Row = Vec<u16>;

/// Span start information
#[derive(Clone, Copy, Debug)]
pub enum C16SpanStart {
    End,
    Colour(u16),
    Transparent(u16),
}

impl C16SpanStart {
    /// Decodes a span start into an actual instruction
    #[inline]
    pub fn decode(v: u16) -> Self {
        if v == 0 {
            Self::End
        } else {
            let run_len = v & 0x7FFF;
            if (v & 0x8000) != 0 {
                Self::Colour(run_len)
            } else {
                Self::Transparent(run_len)
            }
        }
    }
    /// Encodes a span start (make sure the limit of 32767 is not exceeded)
    #[inline]
    pub fn encode(&self) -> u16 {
        match self {
            Self::End => 0,
            Self::Colour(len) => *len | 0x8000,
            Self::Transparent(len) => *len
        }
    }
    /// Instruction data length (in words)
    #[inline]
    pub fn data_len(&self) -> usize {
        match self {
            Self::End => 0,
            Self::Colour(len) => *len as usize,
            Self::Transparent(_) => 0,
        }
    }
    /// Instruction X advance (in pixels)
    #[inline]
    pub fn advance(&self) -> usize {
        match self {
            Self::End => 0,
            Self::Colour(len) => *len as usize,
            Self::Transparent(len) => *len as usize,
        }
    }
}

/// Single frame of a C16 file.
#[derive(Clone)]
pub struct C16Frame {
    pub width: usize,
    /// Rows. It's assumed these were verified, or a panic will occur.
    pub rows: Vec<C16Row>,
}

impl C16Frame {
    /// Compresses a frame.
    pub fn compress(frame: &S16Frame) -> C16Frame {
        C16Frame {
            width: frame.width(),
            rows: (0 .. frame.height()).map(|v| {
                c16_row_compress(&frame.row(v))
            }).collect()
        }
    }

    /// Decompress this frame.
    pub fn decompress(&self) -> S16Frame {
        let mut res = S16Frame::new_filled(self.width, self.rows.len(), 0);
        for (y, row) in self.rows.iter().enumerate() {
            let mut row_iter = row.iter();
            let mut x: usize = 0;
            loop {
                if let Some(v) = row_iter.next() {
                    match C16SpanStart::decode(*v) {
                        C16SpanStart::End => {
                            // wut
                            break;
                        }
                        C16SpanStart::Colour(len) => {
                            for _ in 0..len {
                                res.set_pixel(x, y, *row_iter.next().unwrap());
                                x += 1;
                            }
                        }
                        C16SpanStart::Transparent(len) => {
                            // no need to do anything, already filled to transparent
                            x += len as usize;
                        }
                    }
                } else {
                    break;
                }
            }
        }
        res
    }
}

/// Validates a C16 row.
pub fn c16_row_validate(row: &C16Row, expected_x: usize) -> bool {
    let mut row_iter = row.iter();
    let mut x: usize = 0;
    loop {
        if let Some(v) = row_iter.next() {
            let span = C16SpanStart::decode(*v);
            for _ in 0..span.data_len() {
                if let None = row_iter.next() {
                    return false;
                }
            }
            x += span.advance();
        } else {
            break;
        }
    }
    return x == expected_x;
}

/// Compresses a row for C16
pub fn c16_row_compress(input: &[u16]) -> C16Row {
    let mut res = C16Row::new();
    let mut last_change_index = 0;
    let mut last_change_transparent = false;
    for (i, v) in input.iter().enumerate() {
        let transparent = *v == 0;
        if transparent != last_change_transparent {
            c16_row_compress_add(&mut res, last_change_transparent, &input[last_change_index .. i]);
            last_change_index = i;
            last_change_transparent = transparent;
        }
    }
    c16_row_compress_add(&mut res, last_change_transparent, &input[last_change_index .. input.len()]);
    res
}

fn c16_row_compress_add(row: &mut C16Row, transparent: bool, mut span: &[u16]) {
    while span.len() > 0 {
        let segment_len = if span.len() > 32767 { 32767 } else { span.len() };
        let segment = &span[0 .. segment_len];
        let start = if transparent {
            C16SpanStart::Transparent(segment_len as u16).encode()
        } else {
            C16SpanStart::Colour(segment_len as u16).encode()
        };
        row.push(start);
        if !transparent {
            for v in segment {
                row.push(*v);
            }
        }
        span = &span[segment_len .. span.len()];
    }
}
