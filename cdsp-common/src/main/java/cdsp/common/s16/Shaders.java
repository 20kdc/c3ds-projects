/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package cdsp.common.s16;

/**
 * Contains various shaders as not-shaders.
 */
public class Shaders {
	/**
	 * Converts ints to ARGB with 565 fixup.
	 */
	public static int intsToARGBEmu565(int[] in) {
		int r = clamp(in[0], 0, 255);
		int g = clamp(in[1], 0, 255);
		int b = clamp(in[2], 0, 255);
		int a = clamp(in[3], 0, 255);
		// fixup 565 via bitcopying
		r = (r & 0xF8) | (r >> 5);
		g = (g & 0xFC) | (g >> 6);
		b = (b & 0xF8) | (b >> 5);
		// continue
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	/**
	 * Converts ARGB to floats with 565 emulation.
	 */
	public static void argbToIntsEmu565(int[] out, int argb) {
		// note the use of F8/FC/F8 here
		// this is to properly emulate tint quirks
		out[0] = (argb >> 16) & 0xF8;
		out[1] = (argb >> 8) & 0xFC;
		out[2] = argb & 0xF8;
		out[3] = (argb >> 24) & 0xFF;
	}

	/**
	 * Emulates quirky absolute lerp
	 */
	public static int quirkyAbsLerp(long a, long b, int val) {
		a &= 0xFFFFFFFFL;
		b &= 0xFFFFFFFFL;
		long bCoeff = Math.abs(val - 128);
		// this one quirk makes everything so much more scuffed
		// the emulation of the effects of this one single mistake is still not complete
		long aCoeff = val == 0 ? 65535 : 127 - bCoeff;
		// There's implied overflow potential here.
		long tmp = (a * aCoeff) + (b * bCoeff);
		tmp &= 0xFFFFFFFFL;
		return ((int) tmp) >> 7;
	}

	/**
	 * Rotates an ivec3 (0-255) colour.
	 * Quirks emulation is awkward.
	 */
	public static void rotate(int[] colour, int val) {
		boolean rotateLeft = val >= 128;
		int newR = rotateLeft ? colour[1] : colour[2];
		int newG = rotateLeft ? colour[2] : colour[0];
		int newB = rotateLeft ? colour[0] : colour[1];
		colour[0] = quirkyAbsLerp(colour[0], newR, val);
		colour[1] = quirkyAbsLerp(colour[1], newG, val);
		colour[2] = quirkyAbsLerp(colour[2], newB, val);
	}

	/**
	 * Swaps an ivec3 (0-255) colour.
	 * Quirks emulation is awkward.
	 */
	public static void swap(int[] colour, int val) {
		int tmp = colour[0];
		colour[0] = quirkyAbsLerp(tmp, colour[2], val);
		colour[2] = quirkyAbsLerp(colour[2], tmp, val);
	}

	/**
	 * Clamp colour to 0-255.
	 */
	public static void clampChannelInts(int[] colour) {
		for (int i = 0; i < colour.length; i++)
			colour[i] = clamp(colour[i], 0, 255);
	}

	/**
	 * Clamps a value to the given range.
	 */
	public static float clamp(float a, float min, float max) {
		return Math.min(Math.max(a, min), max);
	}

	/**
	 * Clamps a value to the given range.
	 */
	public static int clamp(int a, int min, int max) {
		return Math.min(Math.max(a, min), max);
	}

	/**
	 * Linearly interpolates between A/B.
	 */
	public static float lerp(float a, float b, float p) {
		return (a * (1 - p)) + (b * p);
	}
}
