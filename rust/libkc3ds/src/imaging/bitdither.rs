// libkc3ds - General-purpose Rust library for C3/DS
// Written starting in 2024 by contributors (see CREDITS.txt)
// To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
// You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

use super::*;

/// Interpolation tables used by bitdither.
/// This table performs interpolation in linear light (internally converts gamma).
pub const INTERPOLATION_TABLES_LL: [[BitCopyInterpolation; 256]; 9] = [
    bitcopy_interpolation_table(0, true),
    bitcopy_interpolation_table(1, true),
    bitcopy_interpolation_table(2, true),
    bitcopy_interpolation_table(3, true),
    bitcopy_interpolation_table(4, true),
    bitcopy_interpolation_table(5, true),
    bitcopy_interpolation_table(6, true),
    bitcopy_interpolation_table(7, true),
    bitcopy_interpolation_table(8, true),
];

/// Interpolation tables used by bitdither
/// This is a 'direct value' table for values that do not need the linear light modulation (i.e. alpha)
pub const INTERPOLATION_TABLES_DV: [[BitCopyInterpolation; 256]; 9] = [
    bitcopy_interpolation_table(0, false),
    bitcopy_interpolation_table(1, false),
    bitcopy_interpolation_table(2, false),
    bitcopy_interpolation_table(3, false),
    bitcopy_interpolation_table(4, false),
    bitcopy_interpolation_table(5, false),
    bitcopy_interpolation_table(6, false),
    bitcopy_interpolation_table(7, false),
    bitcopy_interpolation_table(8, false),
];

/// Method of dithering down by bit-count.
pub trait BitDitherMethod {
    /// Dither down an 8-bit channel to the given bit-count.
    /// Note that this process will always result in bitcopied bits.
    /// This is because this makes for accurate previews and it works nicely with the "floor" methods.
    /// is_alpha may change internal gamma conversion curves.
    fn run(&self, input: Raster<u8>, bits: u8, is_alpha: bool) -> Raster<u8>;
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
    fn run(&self, mut input: Raster<u8>, bits: u8, is_alpha: bool) -> Raster<u8> {
        let interpolation = if is_alpha {
            &INTERPOLATION_TABLES_DV[bits as usize]
        } else {
            &INTERPOLATION_TABLES_LL[bits as usize]
        };
        input.map_inplace(&mut |x, y, v| {
            let ie = &interpolation[v as usize];
            // levels * 2 is used for rounding up, that's then dealt with by the (x+1)>>1
            let level = (((ie.frac_num * self.levels() * 2) / ie.frac_div) + 1) >> 1;
            if self.get(x, y, level) {
                ie.to as u8
            } else {
                ie.from as u8
            }
        });
        input
    }
}

/// Blatant workaround
struct BitDitherMethodFromPattern(&'static dyn DitherPattern);

impl BitDitherMethod for BitDitherMethodFromPattern {
    fn run(&self, input: Raster<u8>, bits: u8, is_alpha: bool) -> Raster<u8> {
        self.0.run(input, bits, is_alpha)
    }
}

/// You know, that method
struct BitDitherMethodFloor();

impl BitDitherMethod for BitDitherMethodFloor {
    fn run(&self, mut input: Raster<u8>, bits: u8, _is_alpha: bool) -> Raster<u8> {
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
        level >= self.2[(x % self.0) + ((y % self.1) * self.0)]
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
    &DitherPatternStatic(2, 2, &[3, 1, 1, 3]);

// From DHALF.txt, source https://github.com/SixLabors/ImageSharp/blob/main/src/ImageSharp/Processing/Processors/Dithering/DHALF.TXT

pub const DITHER_PATTERN_BAYER2: &'static dyn DitherPattern =
    &DitherPatternStatic(2, 2, &[1, 3, 4, 2]);

pub const DITHER_PATTERN_BAYER4: &'static dyn DitherPattern = &DitherPatternStatic(
    4,
    4,
    &[1, 9, 3, 11, 13, 5, 15, 7, 4, 12, 2, 10, 16, 8, 14, 6],
);

/// Dither matrix from Tomeno. These seem really promising for alpha!
pub const DITHER_PATTERN_BLUENOISE9: &'static dyn DitherPattern = &DitherPatternStatic(
    9,
    9,
    &[
        56, 7, 18, 67, 55, 12, 34, 20, 52, 77, 61, 36, 51, 25, 63, 79, 71, 15, 24, 40, 2, 75, 29,
        42, 8, 48, 32, 44, 11, 70, 46, 14, 21, 60, 4, 68, 28, 64, 19, 78, 57, 39, 73, 35, 54, 13,
        50, 33, 5, 53, 9, 65, 17, 80, 59, 37, 72, 62, 26, 31, 49, 23, 1, 74, 10, 22, 43, 16, 69,
        76, 45, 41, 66, 30, 47, 81, 3, 38, 58, 6, 27,
    ],
);

/// Dither matrix from Tomeno. These seem really promising for alpha!
pub const DITHER_PATTERN_BLUENOISE15: &'static dyn DitherPattern = &DitherPatternStatic(
    15,
    15,
    &[
        80, 9, 146, 190, 99, 216, 113, 151, 67, 88, 189, 142, 111, 60, 125, 18, 91, 222, 59, 161,
        45, 141, 56, 104, 23, 154, 5, 74, 30, 155, 194, 166, 128, 24, 85, 122, 181, 31, 223, 174,
        39, 97, 218, 179, 209, 103, 66, 37, 176, 207, 14, 197, 79, 159, 116, 202, 135, 84, 120, 44,
        7, 115, 201, 143, 106, 70, 133, 94, 8, 49, 64, 191, 15, 55, 149, 137, 183, 77, 53, 2, 168,
        42, 213, 184, 145, 130, 27, 163, 175, 90, 33, 21, 158, 96, 220, 150, 119, 22, 101, 75, 217,
        110, 71, 198, 224, 62, 208, 131, 188, 28, 63, 192, 54, 160, 32, 171, 4, 41, 100, 124, 81,
        167, 47, 114, 89, 173, 136, 82, 206, 126, 87, 210, 140, 153, 12, 107, 144, 16, 72, 36, 214,
        6, 108, 17, 182, 58, 112, 187, 51, 203, 93, 193, 221, 180, 152, 123, 199, 68, 147, 46, 156,
        13, 76, 29, 177, 38, 57, 134, 10, 102, 48, 165, 26, 225, 95, 196, 132, 219, 121, 162, 69,
        118, 25, 83, 204, 61, 92, 138, 117, 178, 34, 65, 86, 148, 1, 200, 172, 212, 157, 129, 185,
        169, 40, 78, 11, 164, 105, 20, 215, 98, 139, 50, 109, 35, 73, 19, 3, 195, 211, 127, 52,
        205, 170, 43, 186,
    ],
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

pub const DITHER_PATTERN_BLUENOISE9_RANDOM: &'static dyn DitherPattern =
    &DitherPatternMultiply(DITHER_PATTERN_BLUENOISE9, DITHER_PATTERN_RANDOM);

pub const DITHER_PATTERN_BLUENOISE15_RANDOM: &'static dyn DitherPattern =
    &DitherPatternMultiply(DITHER_PATTERN_BLUENOISE15, DITHER_PATTERN_RANDOM);

/// All bitdither methods
pub const ALL_BITDITHER_METHODS: &[(&str, &'static dyn BitDitherMethod)] = &[
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
    (
        "bluenoise9",
        &BitDitherMethodFromPattern(DITHER_PATTERN_BLUENOISE9),
    ),
    (
        "bluenoise15",
        &BitDitherMethodFromPattern(DITHER_PATTERN_BLUENOISE15),
    ),
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
    (
        "bluenoise9-random",
        &BitDitherMethodFromPattern(DITHER_PATTERN_BLUENOISE9_RANDOM),
    ),
    (
        "bluenoise15-random",
        &BitDitherMethodFromPattern(DITHER_PATTERN_BLUENOISE15_RANDOM),
    ),
];
