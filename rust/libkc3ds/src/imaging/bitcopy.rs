// libkc3ds - General-purpose Rust library for C3/DS
// Written starting in 2024 by contributors (see CREDITS.txt)
// To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
// You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

/// Field for bitcopy
#[derive(Copy, Clone)]
pub struct BitCopyField {
    bits: usize,
    container: usize,
    shift: usize,
    uppermask: usize,
    lowermask: usize
}

/// Interpolation data between two bitcopy values.
#[derive(Copy, Clone)]
pub struct BitCopyInterpolation {
    /// Source
    pub from: usize,
    /// Target
    pub to: usize,
    /// Numerator side of fraction
    pub frac_num: usize,
    /// Divisor side of fraction
    pub frac_div: usize
}

impl BitCopyField {
    pub const fn new(bits: usize, container: usize) -> BitCopyField {
        let shift = container - bits;
        let lowermask = ((usize::MAX) << bits) ^ usize::MAX;
        BitCopyField {
            bits,
            container,
            shift,
            uppermask: lowermask << shift,
            lowermask
        }
    }

    /// Upper bits mask.
    #[inline]
    pub const fn uppermask(&self) -> usize {
        self.uppermask
    }

    /// Lower bits mask.
    #[inline]
    pub const fn lowermask(&self) -> usize {
        self.lowermask
    }

    /// Shift.
    #[inline]
    pub const fn shift(&self) -> usize {
        self.shift
    }

    /// Bits.
    #[inline]
    pub const fn bits(&self) -> usize {
        self.bits
    }

    /// Shifts up and then bitcopies a value.
    #[inline]
    pub const fn shiftup(&self, value: usize) -> usize {
        self.bitcopy(value << self.shift)
    }

    /// Shifts down a value.
    #[inline]
    pub const fn shiftdown(&self, value: usize) -> usize {
        value >> self.shift
    }

    /// Given a value and bitcopy settings, floors it and then bitcopies it downwards.
    pub const fn bitcopy(&self, mut value: usize) -> usize {
        if self.bits == 0 {
            0
        } else {
            let mask = self.uppermask();
            value &= mask;
            // copy while it's possible'
            let mut copyable = value;
            while copyable != 0 {
                copyable >>= self.bits;
                value |= copyable;
            }
            value
        }
    }

    /// Shifts down, goes to the previous value, and bitcopies.
    pub const fn prev(&self, value: usize) -> Option<usize> {
        let low = self.shiftdown(value);
        if low == 0 {
            None
        } else {
            Some(self.shiftup(low - 1))
        }
    }

    /// Shifts down, goes to the next value, and bitcopies.
    pub const fn next(&self, value: usize) -> Option<usize> {
        let low = self.shiftdown(value);
        if low == self.lowermask {
            None
        } else {
            Some(self.shiftup(low + 1))
        }
    }

    /// Gets an interpolation for the given (container-sized) value.
    /// If `interpolate_in_linear_light` is true, assumes that the 8-bit values are in sRGB space.
    /// In this case, calculates interpolations according to a const-friendly aprpoximation of linear light space.
    pub const fn interpolation(&self, value: usize, interpolate_in_linear_light: bool) -> BitCopyInterpolation {
        // This can be greater than the value, i.e:
        // bc = 0x55 is greater than value = 0x50
        // In this case we need to go back a step
        let bc = self.bitcopy(value);
        let here = if bc <= value {
            bc
        } else {
            // this implies there must be something to go back to
            // also we can't unwrap prev()
            self.shiftup(self.shiftdown(bc) - 1)
        };
        let next = self.next(here);
        match next {
            Some(next) => {
                if interpolate_in_linear_light {
                    // here_intensity < value_intensity < next_intensity
                    // but!!! we can't do float ops in const code!
                    // for what we're doing, which is already pretty subtle, a 2.0 power curve is sufficient
                    // notably, these values are raised to X.c2 fixed-point from X.c1 fixed-point
                    // (in practice X.16 from X.8)
                    let here_intensity = here * here;
                    let value_intensity = value * value;
                    let next_intensity = next * next;
                    let intensity_distance = next_intensity - here_intensity;
                    // ok, so, intensity_distance is also in X.c2 space
                    // a direct division would bring it down to X.0 space (unusable since this value is logically 0-1)
                    // assume that X.c1 is is a good gauge of how precise we need to be
                    // position thus is in X.c1 space
                    let position = ((value_intensity - here_intensity) << self.container) / intensity_distance;
                    // confirm that the position is inbounds
                    // since this is handled in const with usize, Rust will complain if the value goes <0
                    let limit = (1 << self.container) - 1;
                    let position_bounded = if position > limit {
                        limit
                    } else {
                        position
                    };
                    BitCopyInterpolation {
                        from: here,
                        to: next,
                        frac_num: position_bounded,
                        // 1.0 in X.c1 space
                        frac_div: 1 << self.container
                    }
                } else {
                    // interpolation point
                    // use exact fractions
                    BitCopyInterpolation {
                        from: here,
                        to: next,
                        frac_num: value - here,
                        frac_div: next - here
                    }
                }
            },
            None => {
                // well, end of the line
                // this should only be 0xFF
                BitCopyInterpolation {
                    from: here,
                    to: here,
                    frac_num: 1,
                    frac_div: 1
                }
            }
        }
    }
}

/// Creates an 8-bit to some amount of bits under or equal to 8 interpolation table.
/// If `interpolate_in_linear_light` is true, assumes that the 8-bit values are in sRGB space.
/// In this case, calculates interpolations according to a const-friendly aprpoximation of linear light space.
pub const fn bitcopy_interpolation_table(bits: usize, interpolate_in_linear_light: bool) -> [BitCopyInterpolation;256] {
    let bcf = BitCopyField::new(bits, 8);
    let mut result: [BitCopyInterpolation;256] = [BitCopyInterpolation {
        from: 0,
        to: 0,
        frac_num: 0,
        frac_div: 1,
    };256];
    let mut i: usize = 0;
    while i < 256 {
        result[i] = bcf.interpolation(i, interpolate_in_linear_light);
        i += 1;
    }
    result
}
