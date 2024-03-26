// libkc3ds - General-purpose Rust library for C3/DS
// Written starting in 2024 by contributors (see CREDITS.txt)
// To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
// You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

use super::super::imaging::*;
use super::*;

// -- outer utils --

/// Identifies a S16/C16.
pub fn identify(data: &[u8]) -> Option<CS16Type> {
    if let Ok(val) = ENDIANNESS_LE.r_u32(data, 0) {
        CS16Type::of_magic(val)
    } else {
        None
    }
}

/// Gets headers for a S16/C16.
pub fn headers(t: CS16Type, data: &[u8]) -> Result<CS16Header, String> {
    match t {
        CS16Type::S16(st) => headers_s16(st, data)
            .map(CS16Header::S16)
            .map_err(|_| "invalid S16 header".into()),
        CS16Type::C16(st) => headers_c16(st, data)
            .map(CS16Header::C16)
            .map_err(|_| "invalid C16 header".into()),
    }
}

/// Identifies and gets headers for a S16/C16.
pub fn identify_and_headers(data: &[u8]) -> Result<CS16Header, String> {
    if let Some(val) = identify(data) {
        headers(val, data)
    } else {
        Err("unable to identify file".into())
    }
}

/// Reads and decompresses a S16/C16.
pub fn read_and_decompress(header: &CS16Header, data: &[u8]) -> Result<CS16Sheet, String> {
    match header {
        CS16Header::S16(st) => read_s16(st, data).map(|v| v.into()),
        CS16Header::C16(st) => read_c16(st, data).map(|v| v.into()),
    }
}

/// Just do absolutely everything and get a CS16Sheet.
pub fn identify_and_decompress(data: &[u8]) -> Result<CS16Sheet, String> {
    read_and_decompress(&identify_and_headers(data)?, data)
}

/// Builds from a CS16Sheet.
pub fn build(t: &CS16Sheet) -> Vec<u8> {
    match t.id {
        CS16Type::S16(s16) => build_s16(&S16Sheet {
            id: s16,
            frames: t.frames.clone(),
        }),
        CS16Type::C16(c16) => build_c16(&C16Sheet {
            id: c16,
            frames: t.frames.iter().map(|v| C16Frame::compress(v)).collect(),
        }),
    }
}

// -- header management --

pub trait CS16HeaderCommon {
    /// Gets CS16 variant.
    fn variant_cs16(&self) -> CS16Type;
    /// Gets a summary of image sizes.
    fn image_sizes(&self) -> Vec<(usize, usize)>;
    /// Measures the size of the header.
    fn size(&self) -> usize;
    /// Converts to bytes.
    fn to_bytes(&self) -> Vec<u8>;
}

#[derive(Clone, Debug)]
pub enum CS16Header {
    S16(S16Header),
    C16(C16Header),
}

impl CS16HeaderCommon for CS16Header {
    fn variant_cs16(&self) -> CS16Type {
        match self {
            Self::S16(s16) => s16.variant_cs16(),
            Self::C16(c16) => c16.variant_cs16(),
        }
    }
    fn image_sizes(&self) -> Vec<(usize, usize)> {
        match self {
            Self::S16(s16) => s16.image_sizes(),
            Self::C16(c16) => c16.image_sizes(),
        }
    }
    fn size(&self) -> usize {
        match self {
            Self::S16(s16) => s16.size(),
            Self::C16(c16) => c16.size(),
        }
    }
    fn to_bytes(&self) -> Vec<u8> {
        match self {
            Self::S16(s16) => s16.to_bytes(),
            Self::C16(c16) => c16.to_bytes(),
        }
    }
}

/// S16 file headers.
#[derive(Clone, Debug)]
pub struct S16Header {
    pub variant: S16Type,
    pub images: Vec<S16HeaderImage>,
}

/// S16 image headers.
#[derive(Clone, Copy, Debug)]
pub struct S16HeaderImage {
    pub base: u32,
    pub width: u16,
    pub height: u16,
}

impl CS16HeaderCommon for S16Header {
    fn variant_cs16(&self) -> CS16Type {
        CS16Type::S16(self.variant)
    }
    fn image_sizes(&self) -> Vec<(usize, usize)> {
        self.images
            .iter()
            .map(|v| (v.width as usize, v.height as usize))
            .collect()
    }
    fn size(&self) -> usize {
        6 + (self.images.len() * 8)
    }
    fn to_bytes(&self) -> Vec<u8> {
        let mut data = vec![0; self.size()];
        let endianness = self.variant.endianness();
        ENDIANNESS_LE.w_u32(&mut data, 0, self.variant.magic());
        endianness.w_u16(&mut data, 4, self.images.len() as u16);
        let mut ptr = 6;
        for img in &self.images {
            endianness.w_u32(&mut data, ptr, img.base);
            endianness.w_u16(&mut data, ptr + 4, img.width);
            endianness.w_u16(&mut data, ptr + 6, img.height);
            ptr += 8;
        }
        data
    }
}

/// Reads S16 headers.
pub fn headers_s16(t: S16Type, data: &[u8]) -> Result<S16Header, ()> {
    let endianness = t.endianness();
    let image_count = endianness.r_u16(data, 4)?;
    let mut headers = S16Header {
        variant: t,
        images: Vec::new(),
    };
    let mut ptr = 6;
    for _ in 0..image_count {
        headers.images.push(S16HeaderImage {
            base: endianness.r_u32(data, ptr)?,
            width: endianness.r_u16(data, ptr + 4)?,
            height: endianness.r_u16(data, ptr + 6)?,
        });
        ptr += 8;
    }
    Ok(headers)
}

/// C16 file headers.
#[derive(Clone, Debug)]
pub struct C16Header {
    pub variant: C16Type,
    pub images: Vec<C16HeaderImage>,
}

/// C16 image headers.
#[derive(Clone, Debug)]
pub struct C16HeaderImage {
    pub base: u32,
    pub width: u16,
    pub height: u16,
    // Row bases for non-1st row
    pub row_bases: Vec<u32>,
}

impl C16HeaderImage {
    /// Creates the header from a base offset and a frame.
    pub fn from_base_and_frame(base: usize, frame: &C16Frame) -> C16HeaderImage {
        let mut row_bases: Vec<u32> = Vec::new();
        let mut ptr = base;
        let mut first_row = true;
        for row in &frame.rows {
            if !first_row {
                row_bases.push(ptr as u32);
            }
            ptr += (row.len() * 2) + 2;
            first_row = false;
        }
        C16HeaderImage {
            base: base as u32,
            width: frame.width as u16,
            height: frame.rows.len() as u16,
            row_bases,
        }
    }
    /// The base gets shifted after creation because it's easier to handle that way. Do that.
    pub fn shift_base(&mut self, base: usize) {
        self.base += base as u32;
        for row in &mut self.row_bases {
            *row += base as u32;
        }
    }
}

impl CS16HeaderCommon for C16Header {
    fn variant_cs16(&self) -> CS16Type {
        CS16Type::C16(self.variant)
    }
    fn image_sizes(&self) -> Vec<(usize, usize)> {
        self.images
            .iter()
            .map(|v| (v.width as usize, v.height as usize))
            .collect()
    }
    fn size(&self) -> usize {
        let mut total = 6 + (self.images.len() * 8);
        for v in &self.images {
            total += v.row_bases.len() * 4;
        }
        total
    }
    fn to_bytes(&self) -> Vec<u8> {
        let mut data = vec![0; self.size()];
        let endianness = self.variant.endianness();
        ENDIANNESS_LE.w_u32(&mut data, 0, self.variant.magic());
        endianness.w_u16(&mut data, 4, self.images.len() as u16);
        let mut ptr = 6;
        for img in &self.images {
            endianness.w_u32(&mut data, ptr, img.base);
            endianness.w_u16(&mut data, ptr + 4, img.width);
            endianness.w_u16(&mut data, ptr + 6, img.height);
            ptr += 8;
            if img.height > 1 {
                assert_eq!(img.row_bases.len(), (img.height - 1) as usize);
                for rb in &img.row_bases {
                    endianness.w_u32(&mut data, ptr, *rb);
                    ptr += 4;
                }
            } else {
                assert_eq!(img.row_bases.len(), 0);
            }
        }
        data
    }
}

pub fn headers_c16(t: C16Type, data: &[u8]) -> Result<C16Header, ()> {
    let endianness = t.endianness();
    let image_count = endianness.r_u16(data, 4)?;
    let mut headers = C16Header {
        variant: t,
        images: Vec::new(),
    };
    let mut ptr = 6;
    for _ in 0..image_count {
        let mut ih = C16HeaderImage {
            base: endianness.r_u32(data, ptr)?,
            width: endianness.r_u16(data, ptr + 4)?,
            height: endianness.r_u16(data, ptr + 6)?,
            row_bases: Vec::new(),
        };
        ptr += 8;
        if ih.height > 1 {
            for _ in 1..ih.height {
                ih.row_bases.push(endianness.r_u32(data, ptr)?);
                ptr += 4;
            }
        }
        headers.images.push(ih);
    }
    Ok(headers)
}

// -- actual readers --

/// Reads a S16.
pub fn read_s16(header: &S16Header, data: &[u8]) -> Result<S16Sheet, String> {
    let endianness = header.variant.endianness();
    let mut res = S16Sheet {
        id: header.variant,
        frames: Vec::new(),
    };
    for ihdr in &header.images {
        let mut frame = S16Frame::new(ihdr.width as usize, ihdr.height as usize);
        let mut ptr = ihdr.base as usize;
        for y in 0..ihdr.height {
            for x in 0..ihdr.width {
                frame.set_pixel(
                    x as usize,
                    y as usize,
                    endianness
                        .r_u16(data, ptr)
                        .map_err(|_| "unable to read pixel")?,
                );
                ptr += 2;
            }
        }
        res.frames.push(frame);
    }
    Ok(res)
}

/// Reads a C16.
pub fn read_c16(header: &C16Header, data: &[u8]) -> Result<C16Sheet, String> {
    let endianness = header.variant.endianness();
    let mut res = C16Sheet {
        id: header.variant,
        frames: Vec::new(),
    };
    for (fidx, ihdr) in header.images.iter().enumerate() {
        let mut frame = C16Frame {
            width: ihdr.width as usize,
            rows: Vec::new(),
        };
        // read each row
        for idx in 0..ihdr.height {
            let row_base = if idx == 0 {
                ihdr.base as usize
            } else {
                ihdr.row_bases[idx as usize - 1] as usize
            };
            frame
                .rows
                .push(read_c16_row(endianness, data, row_base).map_err(|err| {
                    format!(
                        "frame {}: error in row {} (start @ {}): {}",
                        fidx, idx, row_base, err
                    )
                })?);
        }
        res.frames.push(frame);
    }
    Ok(res)
}

/// Reads a C16 row.
/// Beware: The row may not necessarily be valid!
fn read_c16_row(
    endianness: &'static dyn Endianness,
    data: &[u8],
    mut ptr: usize,
) -> Result<C16Row, String> {
    let mut row = C16Row::new();
    loop {
        let vp = endianness.r_u16(data, ptr);
        if let Ok(v) = vp {
            ptr += 2;
            if v == 0 {
                break;
            }
            row.push(v);
            for _ in 0..C16SpanStart::decode(v).data_len() {
                let vp = endianness.r_u16(data, ptr);
                if let Ok(v) = vp {
                    ptr += 2;
                    row.push(v);
                } else {
                    break;
                }
            }
        } else {
            break;
        }
    }
    Ok(row)
}

/// Pushes a u16
pub(crate) fn push_u16(data: &mut Vec<u8>, endianness: &'static dyn Endianness, v: u16) {
    let mut tmp: [u8; 2] = [0; 2];
    endianness.w_u16(&mut tmp, 0, v);
    data.push(tmp[0]);
    data.push(tmp[1]);
}

/// Builds a S16.
pub fn build_s16(sheet: &S16Sheet) -> Vec<u8> {
    let mut headers = S16Header {
        variant: sheet.id,
        images: sheet
            .frames
            .iter()
            .map(|v| S16HeaderImage {
                base: 0,
                width: v.width() as u16,
                height: v.height() as u16,
            })
            .collect(),
    };
    let mut out = headers.to_bytes();
    let endianness = sheet.id.endianness();
    for (idx, frame) in sheet.frames.iter().enumerate() {
        headers.images[idx].base = out.len() as u32;
        for y in 0..frame.height() {
            for v in frame.row(y) {
                push_u16(&mut out, endianness, *v)
            }
        }
    }
    out
}

/// Builds a C16.
pub fn build_c16(sheet: &C16Sheet) -> Vec<u8> {
    let mut headers = C16Header {
        variant: sheet.id,
        images: sheet
            .frames
            .iter()
            .map(|v| C16HeaderImage::from_base_and_frame(0, v))
            .collect(),
    };
    let mut out = headers.to_bytes();
    let endianness = sheet.id.endianness();
    for (idx, frame) in sheet.frames.iter().enumerate() {
        headers.images[idx].shift_base(out.len());
        for row in &frame.rows {
            for v in row {
                push_u16(&mut out, endianness, *v)
            }
            push_u16(&mut out, endianness, 0)
        }
        push_u16(&mut out, endianness, 0)
    }
    out
}
