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

impl BLK16 {
    /// Split a S16Frame into a BLK16.
    pub fn split(variant: BLK16Type, value: &S16Frame) -> BLK16 {
        BLK16 {
            variant,
            blocks: Self::split_raster(value),
        }
    }
    /// Join into a S16Frame.
    pub fn join(&self) -> S16Frame {
        Self::join_raster(&self.blocks)
    }
    /// Split a S16Frame into a raster of tiles.
    pub fn split_raster(value: &S16Frame) -> Raster<BLK16Tile> {
        let bw = if (value.width() % BLK_TILE_SIZE) != 0 {
            (value.width() / BLK_TILE_SIZE) + 1
        } else {
            value.width() / BLK_TILE_SIZE
        };
        let bh = if (value.height() % BLK_TILE_SIZE) != 0 {
            (value.height() / BLK_TILE_SIZE) + 1
        } else {
            value.height() / BLK_TILE_SIZE
        };
        Raster::generate(bw, bh, &mut |x, y| {
            let mut tile = BLK16Tile::default();
            tile.copy(
                &value.region_clipped(
                    x * BLK_TILE_SIZE,
                    y * BLK_TILE_SIZE,
                    BLK_TILE_SIZE,
                    BLK_TILE_SIZE,
                ),
                0,
                0,
            );
            tile
        })
    }
    /// Join a BLK16's blocks raster into a single S16Frame.
    pub fn join_raster(value: &Raster<BLK16Tile>) -> S16Frame {
        let mut raster = S16Frame::new(
            value.width() * BLK_TILE_SIZE,
            value.height() * BLK_TILE_SIZE,
        );
        value.for_each(&mut |x, y, tile| {
            raster.copy(&tile, x * BLK_TILE_SIZE, y * BLK_TILE_SIZE);
        });
        raster
    }
}
