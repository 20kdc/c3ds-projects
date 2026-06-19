// libkc3ds - General-purpose Rust library for C3/DS
// Written starting in 2024 by contributors (see CREDITS.txt)
// To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
// You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

use super::super::imaging::*;
use super::*;

// -- outer utils --

/// Identifies a S16/C16.
pub fn identify(data: &[u8]) -> Option<SprType> {
    if let Ok(val) = ENDIANNESS_LE.r_u32(data, 0) {
        SprType::of_magic(val)
    } else {
        None
    }
}

/// Gets headers for a S16/C16.
pub fn headers(t: SprType, data: &[u8]) -> Result<SprHeader, String> {
    SprHeader::from_bytes(t, false, data).map_err(|_| "invalid header".into())
}

/// Identifies and gets headers for a S16/C16.
pub fn identify_and_headers(data: &[u8]) -> Result<SprHeader, String> {
    if let Some(val) = identify(data) {
        headers(val, data)
    } else {
        Err("unable to identify file".into())
    }
}

/// Just do absolutely everything and get a [SprSheet].
pub fn identify_and_decompress(data: &[u8]) -> Result<SprSheet, String> {
    read_spr(&identify_and_headers(data)?, data)
}

// -- header management --

/// S16 file headers.
#[derive(Clone, Debug)]
pub struct SprHeader {
    pub variant: SprType,
    pub blk_size: Option<(u16, u16)>,
    pub images: Vec<SprHeaderImage>,
}

/// S16 image headers.
#[derive(Clone, Debug)]
pub struct SprHeaderImage {
    pub base: u32,
    pub width: u16,
    pub height: u16,
    /// Row bases for non-1st row in C16.
    /// If not C16, empty.
    pub row_bases: Vec<u32>,
}

impl SprHeader {
    /// Gets a summary of image sizes.
    pub fn image_sizes(&self) -> Vec<(usize, usize)> {
        self.images
            .iter()
            .map(|v| (v.width as usize, v.height as usize))
            .collect()
    }

    /// Measures the size of the header.
    pub fn size(&self) -> usize {
        let mut total = 6 + (self.images.len() * 8);
        if self.blk_size.is_some() {
            total += 4;
        }
        if let SprType::C16(_) = self.variant {
            for v in &self.images {
                total += v.row_bases.len() * 4;
            }
        }
        total
    }

    /// Reads sprite headers from bytes given a known sprite type.
    pub fn from_bytes(t: SprType, is_blk: bool, data: &[u8]) -> Result<SprHeader, ()> {
        let endianness = t.endianness();
        let mut headers = SprHeader {
            variant: t,
            blk_size: None,
            images: Vec::new(),
        };
        let mut ptr = 4;
        if is_blk {
            headers.blk_size = Some((
                endianness.r_u16(data, ptr)?,
                endianness.r_u16(data, ptr + 2)?,
            ));
            ptr += 4;
        }
        let image_count = endianness.r_u16(data, ptr)?;
        ptr += 2;
        match t {
            SprType::S16(_) => {
                for _ in 0..image_count {
                    let mut ih = SprHeaderImage {
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
                if is_blk {
                    // BLK compatibilty workaround.
                    // Offsets in BLK files are fake and incorrect; the engine relies on BLK tiles being in-order.
                    // See also the notice in the Python lib's s16.py about Random's Room etc.
                    let mut simptr = headers.size();
                    for image in &mut headers.images {
                        image.base = simptr as u32;
                        simptr += BLK_TILE_SIZE * BLK_TILE_SIZE * 2;
                    }
                }
            }
            SprType::S32 => {
                for _ in 0..image_count {
                    let ih = SprHeaderImage {
                        base: endianness.r_u32(data, ptr)?,
                        width: endianness.r_u16(data, ptr + 4)?,
                        height: endianness.r_u16(data, ptr + 6)?,
                        row_bases: Vec::new(),
                    };
                    ptr += 8;
                    // Note the lack of a BLK workaround here; BLK32 files are required to have correct offsets.
                    // (A question arises as to why BLK32 files exist when the background could 'just' be one whole PNG.)
                    headers.images.push(ih);
                }
            }
            SprType::C16(_) => {
                for _ in 0..image_count {
                    let mut ih = SprHeaderImage {
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
            }
        }
        Ok(headers)
    }

    /// Converts to bytes.
    pub fn to_bytes(&self) -> Vec<u8> {
        let mut data = vec![0; self.size()];
        let endianness = self.variant.endianness();
        ENDIANNESS_LE.w_u32(&mut data, 0, self.variant.magic());
        let mut ptr = 4;
        if let Some(blk_size) = self.blk_size {
            endianness.w_u16(&mut data, ptr, blk_size.0);
            ptr += 2;
            endianness.w_u16(&mut data, ptr, blk_size.1);
            ptr += 2;
        }
        endianness.w_u16(&mut data, ptr, self.images.len() as u16);
        ptr += 2;
        for img in &self.images {
            endianness.w_u32(&mut data, ptr, img.base);
            endianness.w_u16(&mut data, ptr + 4, img.width);
            endianness.w_u16(&mut data, ptr + 6, img.height);
            ptr += 8;
            if let SprType::C16(_) = self.variant {
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
        }
        data
    }
}

impl SprHeaderImage {
    /// The base gets shifted after creation because it's easier to handle that way. Do that.
    pub fn shift_base(&mut self, base: usize) {
        self.base += base as u32;
        for row in &mut self.row_bases {
            *row += base as u32;
        }
    }
}

// -- actual readers --

/// Reads a sprite file. We do not bother trying to preserve C16Sheet at this point, it's a fruitless endeavour.
pub fn read_spr(header: &SprHeader, data: &[u8]) -> Result<SprSheet, String> {
    let endianness = header.variant.endianness();
    let mut res = SprSheet {
        id: header.variant,
        frames: Vec::new(),
    };
    match header.variant {
        SprType::S16(_) => {
            for ihdr in &header.images {
                let mut frame = SprFrame::new(ihdr.width as usize, ihdr.height as usize);
                let mut ptr = ihdr.base as usize;
                for y in 0..ihdr.height {
                    for x in 0..ihdr.width {
                        frame.set_pixel(
                            x as usize,
                            y as usize,
                            endianness
                                .r_u16(data, ptr)
                                .map_err(|_| "unable to read pixel")?
                                as Pixel,
                        );
                        ptr += 2;
                    }
                }
                res.frames.push(frame);
            }
        }
        SprType::C16(_) => {
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
                res.frames.push(frame.decompress());
            }
        }
        SprType::S32 => {
            for ihdr in &header.images {
                // Note we don't bother trying to chop this.
                let png_data = &data[ihdr.base as usize..];
                res.frames.push(s32png_decode(png_data)?);
            }
        }
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

/// Builds from a SprSheet.
pub fn build(sheet: &SprSheet, blk_size: Option<(u16, u16)>) -> Vec<u8> {
    let endianness = sheet.id.endianness();
    let mut headers = SprHeader {
        variant: sheet.id,
        blk_size,
        images: sheet
            .frames
            .iter()
            .map(|v| SprHeaderImage {
                base: 0,
                width: v.width() as u16,
                height: v.height() as u16,
                row_bases: Vec::new(),
            })
            .collect(),
    };
    let mut out = headers.to_bytes();
    match sheet.id {
        SprType::S16(_) => {
            for (idx, frame) in sheet.frames.iter().enumerate() {
                headers.images[idx].base = out.len() as u32;
                if blk_size.is_some() {
                    // SpriteBuilder writes these with a dodgy offset. We write the same way for compatibility.
                    headers.images[idx].base -= 4;
                }
                for y in 0..frame.height() {
                    for v in frame.row(y) {
                        push_u16(&mut out, endianness, *v as u16)
                    }
                }
            }
        }
        SprType::S32 => {
            for (idx, frame) in sheet.frames.iter().enumerate() {
                headers.images[idx].base = out.len() as u32;
                out.extend(s32png_encode(frame));
            }
        }
        SprType::C16(_) => {
            let compressed: Vec<C16Frame> = sheet
                .frames
                .iter()
                .map(|i| C16Frame::compress(i, sheet.id.to_cm()))
                .collect();
            for (idx, frame) in compressed.iter().enumerate() {
                let row_bases: &mut Vec<u32> = &mut headers.images[idx].row_bases;
                let mut ptr = 0;
                let mut first_row = true;
                for row in &frame.rows {
                    if !first_row {
                        row_bases.push(ptr as u32);
                    }
                    ptr += (row.len() * 2) + 2;
                    first_row = false;
                }
            }
            let mut out = headers.to_bytes();
            for (idx, frame) in compressed.iter().enumerate() {
                headers.images[idx].shift_base(out.len());
                for row in &frame.rows {
                    for v in row {
                        push_u16(&mut out, endianness, *v)
                    }
                    push_u16(&mut out, endianness, 0)
                }
                push_u16(&mut out, endianness, 0)
            }
        }
    }
    let fixed_headers = headers.to_bytes();
    out[0..fixed_headers.len()].copy_from_slice(&fixed_headers);
    out
}
