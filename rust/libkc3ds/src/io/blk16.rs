// libkc3ds - General-purpose Rust library for C3/DS
// Written starting in 2024 by contributors (see CREDITS.txt)
// To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
// You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

use super::super::imaging::*;
use super::*;

/// Identifies a BLK.
pub fn identify(data: &[u8]) -> Option<SprType> {
    if let Ok(val) = ENDIANNESS_LE.r_u32(data, 0) {
        if let Some(val) = SprType::of_magic(val) {
            if val.blk_capable() {
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

/// Identifies and gets headers for a BLK.
pub fn identify_and_headers(data: &[u8]) -> Result<(cs16::SprHeader, (u16, u16)), String> {
    if let Some(id) = identify(data) {
        let header = cs16::SprHeader::from_bytes(id, true, data)
            .map_err(|_| "invalid header".to_string())?;
        let blk_size = header.blk_size.ok_or_else(|| "no blk_size".to_string())?;
        Ok((header, blk_size))
    } else {
        Err("not a valid BLK".to_string())
    }
}

/// Identifies, gets headers for, and then reads a BLK.
pub fn identify_and_read(data: &[u8]) -> Result<BLKSheet, String> {
    let headers = identify_and_headers(data)?;
    let sheet = cs16::read_spr(&headers.0, data)?;
    Ok(BLKSheet::from_frames(
        sheet.id,
        headers.1 .0 as usize,
        headers.1 .1 as usize,
        &sheet.frames,
    ))
}

/// Builds a BLK file.
pub fn build_blk(blk: &BLKSheet) -> Vec<u8> {
    cs16::build(
        &SprSheet {
            id: blk.id,
            frames: blk.build_frames(),
        },
        Some((blk.blocks.width() as u16, blk.blocks.height() as u16)),
    )
}
