// libkc3ds - General-purpose Rust library for C3/DS
// Written starting in 2024 by contributors (see CREDITS.txt)
// To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
// You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

/// IO utilities for the given endianness.
/// Notably, this can be used as a dynamic trait object for generics-less endianness swapping.
/// Don't implement this, it may break later with more functions added.
pub trait Endianness {
    /// Reads an u8.
    #[inline]
    fn r_u8(&self, source: &[u8], pos: usize) -> Result<u8, ()> {
        if pos >= source.len() {
            Err(())
        } else {
            Ok(source[pos])
        }
    }
    /// Reads an u16.
    fn r_u16(&self, source: &[u8], pos: usize) -> Result<u16, ()>;
    /// Reads an u32.
    fn r_u32(&self, source: &[u8], pos: usize) -> Result<u32, ()>;

    /// Reads an i8.
    #[inline]
    fn r_i8(&self, source: &[u8], pos: usize) -> Result<i8, ()> {
        self.r_u8(source, pos).map(|v| v as i8)
    }
    /// Reads an i16.
    #[inline]
    fn r_i16(&self, source: &[u8], pos: usize) -> Result<i16, ()> {
        self.r_u16(source, pos).map(|v| v as i16)
    }
    /// Reads an i32.
    #[inline]
    fn r_i32(&self, source: &[u8], pos: usize) -> Result<i32, ()> {
        self.r_u32(source, pos).map(|v| v as i32)
    }

    /// Writes an u8.
    #[inline]
    fn w_u8(&self, source: &mut [u8], pos: usize, v: u8) {
        source[pos] = v;
    }
    /// Writes an u16.
    fn w_u16(&self, source: &mut [u8], pos: usize, v: u16);
    /// Writes an u32.
    fn w_u32(&self, source: &mut [u8], pos: usize, v: u32);

    /// Writes an i8.
    #[inline]
    fn w_i8(&self, source: &mut [u8], pos: usize, v: i8) {
        self.w_u8(source, pos, v as u8)
    }
    /// Writes an i16.
    #[inline]
    fn w_i16(&self, source: &mut [u8], pos: usize, v: i16) {
        self.w_u16(source, pos, v as u16)
    }
    /// Writes an i32.
    #[inline]
    fn w_i32(&self, source: &mut [u8], pos: usize, v: i32) {
        self.w_u32(source, pos, v as u32)
    }
}

/// Little-endian endianness
pub struct EndiannessLE();

/// Big-endian endianness
pub struct EndiannessBE();

impl Endianness for EndiannessLE {
    #[inline]
    fn r_u16(&self, source: &[u8], pos: usize) -> Result<u16, ()> {
        let lower = self.r_u8(source, pos)? as u16;
        let upper = self.r_u8(source, pos + 1)? as u16;
        Ok(lower | (upper << 8))
    }
    #[inline]
    fn r_u32(&self, source: &[u8], pos: usize) -> Result<u32, ()> {
        let lower = self.r_u16(source, pos)? as u32;
        let upper = self.r_u16(source, pos + 2)? as u32;
        Ok(lower | (upper << 16))
    }
    #[inline]
    fn w_u16(&self, source: &mut [u8], pos: usize, v: u16) {
        source[pos .. pos + 2].swap_with_slice(v.to_le_bytes().as_mut_slice());
    }
    #[inline]
    fn w_u32(&self, source: &mut [u8], pos: usize, v: u32) {
        source[pos .. pos + 4].swap_with_slice(v.to_le_bytes().as_mut_slice());
    }
}

impl Endianness for EndiannessBE {
    #[inline]
    fn r_u16(&self, source: &[u8], pos: usize) -> Result<u16, ()> {
        let lower = self.r_u8(source, pos + 1)? as u16;
        let upper = self.r_u8(source, pos)? as u16;
        Ok(lower | (upper << 8))
    }
    #[inline]
    fn r_u32(&self, source: &[u8], pos: usize) -> Result<u32, ()> {
        let lower = self.r_u16(source, pos + 2)? as u32;
        let upper = self.r_u16(source, pos)? as u32;
        Ok(lower | (upper << 16))
    }
    #[inline]
    fn w_u16(&self, source: &mut [u8], pos: usize, v: u16) {
        source[pos .. pos + 2].swap_with_slice(v.to_be_bytes().as_mut_slice());
    }
    #[inline]
    fn w_u32(&self, source: &mut [u8], pos: usize, v: u32) {
        source[pos .. pos + 4].swap_with_slice(v.to_be_bytes().as_mut_slice());
    }
}
