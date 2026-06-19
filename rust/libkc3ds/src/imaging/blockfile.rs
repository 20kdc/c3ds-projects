// libkc3ds - General-purpose Rust library for C3/DS
// Written starting in 2024 by contributors (see CREDITS.txt)
// To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
// You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

use super::*;

/// Size of a BLK tile
pub const BLK_TILE_SIZE: usize = 128;

/// BLK tiles are RasterTile
pub type BLKTile = RasterTile<Pixel, BLK_TILE_SIZE, BLK_TILE_SIZE>;

/// BLK file data.
#[derive(Clone)]
pub struct BLKSheet {
    /// Type of S16.
    /// BLK files inherit everything from S16.
    pub id: SprType,
    pub blocks: Raster<BLKTile>,
}

impl BLKSheet {
    pub fn from_frames(id: SprType, width: usize, height: usize, source: &[SprFrame]) -> Self {
        let blocks: Raster<BLKTile> = Raster::generate(width, height, &mut |x, y| {
            let idx = y + (x * height);
            let mut tile = BLKTile::default();
            if idx < source.len() {
                tile.copy_clipped(&source[idx], 0, 0);
            }
            tile
        });
        Self { id, blocks }
    }

    pub fn build_frames(&self) -> Vec<SprFrame> {
        let mut total = Vec::new();
        for x in 0..self.blocks.width() {
            for y in 0..self.blocks.height() {
                total.push(self.blocks.pixel(x, y).to_raster());
            }
        }
        total
    }

    /// Split a SprFrame into a BLKSheet.
    pub fn split(id: SprType, value: &SprFrame) -> Self {
        BLKSheet {
            id,
            blocks: Self::split_raster(value),
        }
    }

    /// Join into a S16Frame.
    pub fn join(&self) -> SprFrame {
        Self::join_raster(&self.blocks)
    }

    /// Split a S16Frame into a raster of tiles.
    pub fn split_raster(value: &SprFrame) -> Raster<BLKTile> {
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
            let mut tile = BLKTile::default();
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
    pub fn join_raster(value: &Raster<BLKTile>) -> SprFrame {
        let mut raster = SprFrame::new(
            value.width() * BLK_TILE_SIZE,
            value.height() * BLK_TILE_SIZE,
        );
        value.for_each(&mut |x, y, tile| {
            raster.copy(&tile, x * BLK_TILE_SIZE, y * BLK_TILE_SIZE);
        });
        raster
    }
}
