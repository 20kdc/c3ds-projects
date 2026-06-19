// libkc3ds - General-purpose Rust library for C3/DS
// Written starting in 2024 by contributors (see CREDITS.txt)
// To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
// You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

use super::{ColourModel, Pixel, RasterishMutObj, RasterishObj, SprFrame};

/// A row, not including end-of-row markers or the end-of-image marker
/// Beware, these may not be well-formed!
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
            let run_len = (v & 0xFFFE) >> 1;
            if (v & 1) != 0 {
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
            Self::Colour(len) => (*len << 1) | 1,
            Self::Transparent(len) => *len << 1,
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
    pub fn compress(frame: &SprFrame, cm: &dyn ColourModel) -> C16Frame {
        C16Frame {
            width: frame.width(),
            rows: (0..frame.height())
                .map(|v| c16_row_compress(frame.row(v), cm))
                .collect(),
        }
    }

    /// Decompress this frame.
    pub fn decompress(&self) -> SprFrame {
        let mut res = SprFrame::new_filled(self.width, self.rows.len(), 0);
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
                                // silently ignore pixels out of range, just in case
                                if x >= self.width {
                                    break;
                                }
                                let pd = row_iter.next();
                                if let Some(px) = pd {
                                    res.set_pixel(x, y, *px as Pixel);
                                } else {
                                    break;
                                }
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

/// Compresses a row for C16
pub fn c16_row_compress(input: &[Pixel], cm: &dyn ColourModel) -> C16Row {
    let mut res = C16Row::new();
    let mut last_change_index = 0;
    let mut last_change_transparent = false;
    for (i, v) in input.iter().enumerate() {
        let transparent = cm.is_sprite_transparency(*v);
        if transparent != last_change_transparent {
            c16_row_compress_add(
                &mut res,
                last_change_transparent,
                &input[last_change_index..i],
            );
            last_change_index = i;
            last_change_transparent = transparent;
        }
    }
    c16_row_compress_add(
        &mut res,
        last_change_transparent,
        &input[last_change_index..input.len()],
    );
    res
}

fn c16_row_compress_add(row: &mut C16Row, transparent: bool, mut span: &[Pixel]) {
    while !span.is_empty() {
        let segment_len = if span.len() > 32767 {
            32767
        } else {
            span.len()
        };
        let segment = &span[0..segment_len];
        let start = if transparent {
            C16SpanStart::Transparent(segment_len as u16).encode()
        } else {
            C16SpanStart::Colour(segment_len as u16).encode()
        };
        row.push(start);
        if !transparent {
            for v in segment {
                row.push(*v as u16);
            }
        }
        span = &span[segment_len..span.len()];
    }
}
