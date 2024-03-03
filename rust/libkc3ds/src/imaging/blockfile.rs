// libkc3ds - General-purpose Rust library for C3/DS
// Written starting in 2024 by contributors (see CREDITS.txt)
// To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
// You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

use super::*;

/// Size of a BLK tile
pub const BLK_TILE_SIZE: usize = 128;

/// BLK types are S16 types
pub type BLK16Type = S16Type;

/// BLK tiles are RasterTile
pub type BLK16Tile = RasterTile<u16, BLK_TILE_SIZE, BLK_TILE_SIZE>;

/// BLK file data.
#[derive(Clone)]
pub struct BLK16 {
    /// Type of S16.
    /// BLK files inherit their endianness and colour model from S16.
    pub variant: BLK16Type,
    pub blocks: Raster<BLK16Tile>,
}
