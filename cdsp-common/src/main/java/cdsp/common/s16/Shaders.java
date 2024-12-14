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
	 * Converts floats to ARGB.
	 */
	public static int floatsToARGB(float[] in) {
		int r = clamp((int) (in[0] * 255), 0, 255);
		int g = clamp((int) (in[1] * 255), 0, 255);
		int b = clamp((int) (in[2] * 255), 0, 255);
		int a = clamp((int) (in[3] * 255), 0, 255);
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	/**
	 * Converts ARGB to floats.
	 */
	public static void argbToFloats(float[] out, int argb) {
		out[0] = ((argb >> 16) & 0xFF) / 255.0f;
		out[1] = ((argb >> 8) & 0xFF) / 255.0f;
		out[2] = (argb & 0xFF) / 255.0f;
		out[3] = ((argb >> 24) & 0xFF) / 255.0f;
	}

	/**
	 * Rotates a vec3.
	 * Rotation is in range -1 to 1.
	 */
	public static void rotate(float[] colour, float v) {
		float newR = v < 0 ? colour[2] : colour[1];
		float newG = v < 0 ? colour[0] : colour[2];
		float newB = v < 0 ? colour[1] : colour[0];
		colour[0] = lerp(colour[0], newR, Math.abs(v));
		colour[1] = lerp(colour[1], newG, Math.abs(v));
		colour[2] = lerp(colour[2], newB, Math.abs(v));
	}

	/**
	 * Swaps a vec3.
	 * Swap is in range 0 to 1.
	 */
	public static void swap(float[] colour, float v) {
		float tmp = colour[0];
		colour[0] = lerp(tmp, colour[2], v);
		colour[2] = lerp(tmp, colour[2], 1 - v);
	}

	/**
	 * Clamp colour to region.
	 */
	public static void clampFloats(float[] colour) {
		for (int i = 0; i < colour.length; i++)
			colour[i] = clamp(colour[i], 0, 1);
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
