// libkc3ds - General-purpose Rust library for C3/DS
// Written starting in 2024 by contributors (see CREDITS.txt)
// To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
// You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

// - todo -

use lebe::prelude::*;
use std::{
    io::Error,
    io::{Read, Seek},
};

use crate::{
    colours::{ColourModel16, RGB1555_INSTANCE, RGB5551_INSTANCE, RGB565_INSTANCE},
    img16::*,
};

struct ModRead<'t, I: Read + Seek> {
    input: &'t mut I,
    big_endian: bool,
}

impl<'t, I: Read + Seek> ModRead<'t, I> {
    fn seek_to(&mut self, u: u64) -> Result<u64, Error> {
        self.input.seek(std::io::SeekFrom::Start(u))
    }
    fn read_u16(&mut self) -> Result<u16, Error> {
        if self.big_endian {
            self.input.read_from_big_endian()
        } else {
            self.input.read_from_little_endian()
        }
    }
    fn read_u32(&mut self) -> Result<u32, Error> {
        if self.big_endian {
            self.input.read_from_big_endian()
        } else {
            self.input.read_from_little_endian()
        }
    }
}

pub fn read_cs16<I: Read + Seek>(mut input: &mut I) -> Result<Vec<S16Frame>, Error> {
    let res = u32::read_from_little_endian(&mut input)?;
    match res {
        // S16 555
        0 => {
            let mut modread = ModRead {
                input,
                big_endian: false,
            };
            read_s16_innards(&mut modread, &RGB1555_INSTANCE)
        }
        // S16 565
        1 => {
            let mut modread = ModRead {
                input,
                big_endian: false,
            };
            read_s16_innards(&mut modread, &RGB565_INSTANCE)
        }
        // C16 555
        2 => {
            let mut modread = ModRead {
                input,
                big_endian: false,
            };
            read_c16_innards(&mut modread, &RGB1555_INSTANCE)
        }
        // C16 565
        3 => {
            let mut modread = ModRead {
                input,
                big_endian: false,
            };
            read_c16_innards(&mut modread, &RGB565_INSTANCE)
        }
        // N16
        0x01000000 => {
            let mut modread = ModRead {
                input,
                big_endian: true,
            };
            read_s16_innards(&mut modread, &RGB5551_INSTANCE)
        }
        // M16
        0x03000000 => {
            let mut modread = ModRead {
                input,
                big_endian: true,
            };
            read_s16_innards(&mut modread, &RGB5551_INSTANCE)
        }
        _ => Err(Error::new(
            std::io::ErrorKind::InvalidData,
            "Invalid magic number",
        )),
    }
}

fn read_s16_innards<I: Read + Seek>(
    input: &mut ModRead<I>,
    model: &'static dyn ColourModel16,
) -> Result<Vec<S16Frame>, Error> {
    let mut frames: Vec<S16Frame> = Vec::new();
    let mut offsets: Vec<u64> = Vec::new();
    let count = input.read_u16()?;
    for _ in 0..count {
        let offset = input.read_u32()?;
        let w = input.read_u16()? as usize;
        let h = input.read_u16()? as usize;
        frames.push(S16Frame::new((w, h), model));
        offsets.push(offset as u64);
    }
    for i in 0..count {
        let frame = &mut frames[i as usize];
        let mut rgn = frame.open_mut();
        if rgn.width() == 0 || rgn.height() == 0 {
            continue;
        }
        input.seek_to(offsets[i as usize])?;
        for i in 0..(rgn.width() * rgn.height()) {
            rgn[i] = input.read_u16()?;
        }
    }
    Ok(frames)
}

fn read_c16_innards<I: Read + Seek>(
    input: &mut ModRead<I>,
    model: &'static dyn ColourModel16,
) -> Result<Vec<S16Frame>, Error> {
    let mut frames: Vec<S16Frame> = Vec::new();
    let mut offsets: Vec<Vec<u64>> = Vec::new();
    let count = input.read_u16()?;
    for _ in 0..count {
        let mut frame_offsets: Vec<u64> = Vec::new();
        frame_offsets.push(input.read_u32()? as u64);
        let w = input.read_u16()? as usize;
        let h = input.read_u16()? as usize;
        for _ in 0..h - 1 {
            frame_offsets.push(input.read_u32()? as u64);
        }
        frames.push(S16Frame::new((w, h), model));
        offsets.push(frame_offsets);
    }
    for i in 0..count {
        let frame = &mut frames[i as usize];
        let mut rgn = frame.open_mut();
        if rgn.width() == 0 || rgn.height() == 0 {
            continue;
        }
        for y in 0..rgn.height() {
            input.seek_to(offsets[i as usize][y])?;
            let len = rgn.width();
            let mut row = rgn.crop_mut((0, y), (len, 1));
            let mut pos = 0;
            let mut remaining = len;
            loop {
                let runhdr = input.read_u16()?;
                let runlen = (runhdr >> 1) as usize;
                if runhdr == 0 {
                    // terminator
                    break;
                }
                if runlen > remaining {
                    // run length is too far, invalid data
                    return Err(Error::new(
                        std::io::ErrorKind::InvalidData,
                        "Run ran off end",
                    ));
                }
                if (runhdr & 1) == 1 {
                    // colour
                    // (transparent is handled just by doing nothing)
                    for j in 0..runlen {
                        row[pos + j] = input.read_u16()?;
                    }
                }
                pos += runlen;
                remaining -= runlen;
            }
        }
    }
    Ok(frames)
}
