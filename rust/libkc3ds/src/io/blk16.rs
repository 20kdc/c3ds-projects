// libkc3ds - General-purpose Rust library for C3/DS
// Written starting in 2024 by contributors (see CREDITS.txt)
// To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
// You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

use super::super::imaging::*;
use super::*;

/// Identifies a BLK.
pub fn identify(data: &[u8]) -> Option<BLK16Type> {
    if let Ok(val) = ENDIANNESS_LE.r_u32(data, 0) {
        if let Some(val) = CS16Type::of_magic(val) {
            if let CS16Type::S16(val) = val {
                Some(val)
            } else {
                None
            }
        } else {
            None
        }
    } else {
        None
    }
}

/// BLK header.
/// The redundant (and sometimes incorrect!) data is ignored.
pub struct BLK16Header {
    pub variant: BLK16Type,
    pub width: u16,
    pub height: u16,
}

impl BLK16Header {
    /// Gets the size of the BLK16 header.
    pub fn size(&self) -> usize {
        10 + ((self.width as usize) * (self.height as usize) * 8)
    }
    /// Header as bytes.
    pub fn to_bytes(&self) -> Vec<u8> {
        let mut data = vec![0; self.size()];
        let endianness = self.variant.endianness();
        ENDIANNESS_LE.w_u32(&mut data, 0, self.variant.magic());
        let imgc = (self.width as usize) * (self.height as usize);
        endianness.w_u16(&mut data, 4, self.width);
        endianness.w_u16(&mut data, 6, self.height);
        endianness.w_u16(&mut data, 8, imgc as u16);
        let mut ptr = 10;
        // *deliberately* incorrect to match how these files are supposed to be written
        // engine doesn't care'
        let mut calculated_base = 6;
        for _ in 0 .. imgc {
            endianness.w_u32(&mut data, ptr, calculated_base as u32);
            endianness.w_u16(&mut data, ptr + 4, BLK_TILE_SIZE as u16);
            endianness.w_u16(&mut data, ptr + 6, BLK_TILE_SIZE as u16);
            ptr += 8;
            calculated_base += BLK_TILE_SIZE * BLK_TILE_SIZE;
        }
        data
    }
}

/// Gets headers for a BLK.
pub fn headers(t: BLK16Type, data: &[u8]) -> Result<BLK16Header, ()> {
    let endianness = t.endianness();
    Ok(BLK16Header {
        variant: t,
        width: endianness.r_u16(data, 4)?,
        height: endianness.r_u16(data, 6)?,
    })
}

struct BLKCoordinateIterator {
    x: u16,
    y: u16,
    w: u16,
    h: u16,
}

impl Iterator for BLKCoordinateIterator {
    type Item = (u16, u16);
    fn next(&mut self) -> Option<Self::Item> {
        let vx = self.x;
        let vy = self.y;
        if vx >= self.w {
            None
        } else {
            self.y += 1;
            if self.y >= self.h {
                self.y = 0;
                self.x += 1;
            }
            Some((vx, vy))
        }
    }
}

/// Reads a BLK.
pub fn read_blk(header: &BLK16Header, data: &[u8]) -> Result<BLK16, ()> {
    let mut ptr = header.size();
    let mut blk = BLK16 {
        variant: header.variant,
        blocks: Raster::new(header.width as usize, header.height as usize)
    };
    let endianness = blk.variant.endianness();
    for (x, y) in (BLKCoordinateIterator { x: 0, y: 0, w: header.width, h: header.height }) {
        let tile = &mut blk.blocks.pixel_mut(x as usize, y as usize);
        for row in &mut tile.0 {
            for pixel in row {
                *pixel = endianness.r_u16(data, ptr)?;
                ptr += 2;
            }
        }
    }
    Ok(blk)
}

/// Builds a BLK.
pub fn build_blk(blk: BLK16) -> Vec<u8> {
    let header = BLK16Header {
        variant: blk.variant,
        width: blk.blocks.width() as u16,
        height: blk.blocks.height() as u16
    };
    let mut data = header.to_bytes();
    let endianness = blk.variant.endianness();
    for (x, y) in (BLKCoordinateIterator { x: 0, y: 0, w: header.width, h: header.height }) {
        let tile = blk.blocks.pixel(x as usize, y as usize);
        for row in &tile.0 {
            for pixel in row {
                cs16::push_u16(&mut data, endianness, *pixel);
            }
        }
    }
    data
}
