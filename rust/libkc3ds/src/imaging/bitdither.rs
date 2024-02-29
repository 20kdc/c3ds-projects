// libkc3ds - General-purpose Rust library for C3/DS
// Written starting in 2024 by contributors (see CREDITS.txt)
// To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
// You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

use super::*;

/// Linear light interpolation
const INTERPOLATE_LL: bool = true;

/// Interpolation tables used by bitdither
pub const INTERPOLATION_TABLES: [[BitCopyInterpolation; 256]; 9] = [
    bitcopy_interpolation_table(0, INTERPOLATE_LL),
    bitcopy_interpolation_table(1, INTERPOLATE_LL),
    bitcopy_interpolation_table(2, INTERPOLATE_LL),
    bitcopy_interpolation_table(3, INTERPOLATE_LL),
    bitcopy_interpolation_table(4, INTERPOLATE_LL),
    bitcopy_interpolation_table(5, INTERPOLATE_LL),
    bitcopy_interpolation_table(6, INTERPOLATE_LL),
    bitcopy_interpolation_table(7, INTERPOLATE_LL),
    bitcopy_interpolation_table(8, INTERPOLATE_LL),
];

/// Method of dithering down by bit-count.
pub trait BitDitherMethod {
    /// Dither down an 8-bit channel to the given bit-count.
    /// Note that this process will always result in bitcopied bits.
    /// This is because this makes for accurate previews and it works nicely with the "floor" methods.
    fn run(&self, input: Raster<u8>, bits: u8) -> Raster<u8>;
}

use std::hash::Hasher;

/// Dithering pattern.
pub trait DitherPattern: BitDitherMethod {
    /// Amount of levels in this dither pattern.
    fn levels(&self) -> usize;
    /// For the given absolute X/Y (looping is handled internally), and a level (0..levels() + 1), return the dither pattern result.
    /// Note that level = levels() is valid. This makes calculations simpler in some cases.
    /// level = 0 should always return all-false.
    /// level = levels() should always return all-true.
    fn get(&self, x: usize, y: usize, level: usize) -> bool;

    /// Upcast to BitDitherMethod
    fn upcast(&self) -> &dyn BitDitherMethod;
}

impl<T: DitherPattern> BitDitherMethod for T {
    fn run(&self, mut input: Raster<u8>, bits: u8) -> Raster<u8> {
        let interpolation = &INTERPOLATION_TABLES[bits as usize];
        input.map_inplace(&mut |x, y, v| {
            let ie = &interpolation[v as usize];
            let level = (ie.frac_num * self.levels()) / ie.frac_div;
            if self.get(x, y, level) {
                ie.from as u8
            } else {
                ie.to as u8
            }
        });
        input
    }
}

/// Blatant workaround
struct BitDitherMethodFromPattern(&'static dyn DitherPattern);

impl BitDitherMethod for BitDitherMethodFromPattern {
    fn run(&self, input: Raster<u8>, bits: u8) -> Raster<u8> {
        self.0.run(input, bits)
    }
}

/// You know, that method
struct BitDitherMethodFloor();

impl BitDitherMethod for BitDitherMethodFloor {
    fn run(&self, mut input: Raster<u8>, bits: u8) -> Raster<u8> {
        let bcf = BitCopyField::new(bits as usize, 8);
        input.map_inplace(&mut |_, _, v| bcf.bitcopy(v as usize) as u8);
        input
    }
}

/// Static dithering pattern.
struct DitherPatternStatic<'a>(usize, usize, &'a [usize]);

impl<'a> DitherPattern for DitherPatternStatic<'a> {
    fn levels(&self) -> usize {
        self.0 * self.1
    }
    fn get(&self, x: usize, y: usize, level: usize) -> bool {
        self.2[x + (y * self.0)] >= level
    }

    fn upcast(&self) -> &dyn BitDitherMethod {
        self
    }
}

/// Multiplicative dither pattern
struct DitherPatternMultiply<'a>(&'a dyn DitherPattern, &'a dyn DitherPattern);

impl<'a> DitherPattern for DitherPatternMultiply<'a> {
    fn levels(&self) -> usize {
        self.0.levels() * self.1.levels()
    }
    fn get(&self, x: usize, y: usize, level: usize) -> bool {
        let div = self.1.levels();
        let inner_bool = self.1.get(x, y, level % div);
        let outer_level_base = level / div;
        if inner_bool {
            self.0.get(x, y, outer_level_base + 1)
        } else {
            self.0.get(x, y, outer_level_base)
        }
    }

    fn upcast(&self) -> &dyn BitDitherMethod {
        self
    }
}

/// Random dither pattern
struct DitherPatternRandom();

impl DitherPattern for DitherPatternRandom {
    fn levels(&self) -> usize {
        256
    }
    fn get(&self, x: usize, y: usize, level: usize) -> bool {
        let mut sh = std::collections::hash_map::DefaultHasher::new();
        sh.write_usize(x);
        sh.write_usize(y);
        sh.write_usize(level);
        (level as u64) > (sh.finish() & 0xFF)
    }

    fn upcast(&self) -> &dyn BitDitherMethod {
        self
    }
}

pub const DITHER_PATTERN_NEAREST: &'static dyn DitherPattern = &DitherPatternStatic(1, 1, &[1]);

pub const DITHER_PATTERN_CHECKERS: &'static dyn DitherPattern =
    &DitherPatternStatic(2, 2, &[4, 2, 2, 4]);

// From DHALF.txt, source https://github.com/SixLabors/ImageSharp/blob/main/src/ImageSharp/Processing/Processors/Dithering/DHALF.TXT

pub const DITHER_PATTERN_BAYER2: &'static dyn DitherPattern =
    &DitherPatternStatic(2, 2, &[1, 3, 4, 2]);

pub const DITHER_PATTERN_BAYER4: &'static dyn DitherPattern = &DitherPatternStatic(
    4,
    4,
    &[1, 9, 3, 11, 13, 5, 15, 7, 4, 12, 2, 10, 16, 8, 14, 6],
);

// All the random combinations

pub const DITHER_PATTERN_RANDOM: &'static dyn DitherPattern = &DitherPatternRandom();

pub const DITHER_PATTERN_NEAREST_RANDOM: &'static dyn DitherPattern =
    &DitherPatternMultiply(DITHER_PATTERN_NEAREST, DITHER_PATTERN_RANDOM);

pub const DITHER_PATTERN_CHECKERS_RANDOM: &'static dyn DitherPattern =
    &DitherPatternMultiply(DITHER_PATTERN_CHECKERS, DITHER_PATTERN_RANDOM);

pub const DITHER_PATTERN_BAYER2_RANDOM: &'static dyn DitherPattern =
    &DitherPatternMultiply(DITHER_PATTERN_BAYER2, DITHER_PATTERN_RANDOM);

pub const DITHER_PATTERN_BAYER4_RANDOM: &'static dyn DitherPattern =
    &DitherPatternMultiply(DITHER_PATTERN_BAYER4, DITHER_PATTERN_RANDOM);

/// All bitdither methods
pub const ALL_BITDITHER_METHODS: &[(&'static str, &'static dyn BitDitherMethod)] = &[
    ("floor", &BitDitherMethodFloor()),
    (
        "nearest",
        &BitDitherMethodFromPattern(DITHER_PATTERN_NEAREST),
    ),
    (
        "checkers",
        &BitDitherMethodFromPattern(DITHER_PATTERN_CHECKERS),
    ),
    ("bayer2", &BitDitherMethodFromPattern(DITHER_PATTERN_BAYER2)),
    ("bayer4", &BitDitherMethodFromPattern(DITHER_PATTERN_BAYER4)),
    ("random", &BitDitherMethodFromPattern(DITHER_PATTERN_RANDOM)),
    (
        "nearest-random",
        &BitDitherMethodFromPattern(DITHER_PATTERN_NEAREST_RANDOM),
    ),
    (
        "checkers-random",
        &BitDitherMethodFromPattern(DITHER_PATTERN_CHECKERS_RANDOM),
    ),
    (
        "bayer2-random",
        &BitDitherMethodFromPattern(DITHER_PATTERN_BAYER2_RANDOM),
    ),
    (
        "bayer4-random",
        &BitDitherMethodFromPattern(DITHER_PATTERN_BAYER4_RANDOM),
    ),
];
