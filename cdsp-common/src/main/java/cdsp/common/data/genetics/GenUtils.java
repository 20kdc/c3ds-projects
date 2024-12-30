/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package cdsp.common.data.genetics;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import cdsp.common.data.VirtualCatalogue;

/**
 * Utilities for reading a .gen file.
 * These work with 'uncanonized' genetics. This is important for emulating various quirks the game can create.
 */
public class GenUtils {
	/**
	 * Strips the header from a genome file.
	 * Returns null on unidentified (non-genome file)
	 */
	public static GenVersion identify(File file) throws IOException {
		return identify(Files.readAllBytes(file.toPath()));
	}

	/**
	 * Identifies the genome type of a genome file.
	 * Returns null on unidentified (non-genome file)
	 */
	public static GenVersion identify(byte[] data) {
		if (matchWord(data, 0, 'd', 'n', 'a', '3'))
			return GenVersion.C3;
		if (matchWord(data, 0, 'd', 'n', 'a', '2'))
			return GenVersion.C2;
		if (matchWord(data, 0, 'g', 'e', 'n', 'e'))
			return GenVersion.C1;
		return null;
	}

	/**
	 * Strips the header from a genome file.
	 */
	public static GenPackage readGenome(File file) throws IOException {
		return readGenome(Files.readAllBytes(file.toPath()));
	}

	/**
	 * Strips the header from a genome file.
	 * May return the passed-in array.
	 */
	public static GenPackage readGenome(byte[] data) {
		GenVersion gv = identify(data);
		if (gv == GenVersion.C1) {
			return new GenPackage(gv, data);
		} else if (gv == null) {
			throw new RuntimeException("Unknown genome version.");
		} else {
			byte[] outb = new byte[data.length - 4];
			System.arraycopy(data, 4, outb, 0, outb.length);
			return new GenPackage(gv, outb);
		}
	}

	/**
	 * Returns the offset of the next 'gene' or 'gend'.
	 * Returns data.length if we run out of data.
	 */
	public static int nextChunk(byte[] data, int start) {
		int effectiveLen = data.length - 4;
		while (start <= effectiveLen) {
			if (data[start] == 'g' && data[start + 1] == 'e' && data[start + 2] == 'n' && (data[start + 3] == 'e' || data[start + 3] == 'd'))
				return start;
			start++;
		}
		return data.length;
	}

	/**
	 * Returns the offset of the next 'gene'.
	 * Returns data.length if we run out of data or if 'gend' is found.
	 */
	public static int nextGene(byte[] data, int start) {
		int effectiveLen = data.length - 4;
		while (start <= effectiveLen) {
			if (data[start] == 'g' && data[start + 1] == 'e' && data[start + 2] == 'n') {
				if (data[start + 3] == 'e')
					return start;
				if (data[start + 3] == 'd')
					return data.length;
			}
			start++;
		}
		return data.length;
	}

	/**
	 * Confirms that a word in the genome matches.
	 */
	public static boolean matchWord(byte[] data, int ptr, char a, char b, char c, char d) {
		if (ptr + 4 > data.length)
			return false;
		if (data[ptr++] != a)
			return false;
		if (data[ptr++] != b)
			return false;
		if (data[ptr++] != c)
			return false;
		return data[ptr] == d;
	}

	/**
	 * Safe get (for gene data)
	 */
	public static int safeGet(byte[] data, int ptr) {
		if (ptr < 0 || ptr >= data.length)
			return 0;
		return data[ptr] & 0xFF;
	}

	/**
	 * Safe get (for gene data) within range.
	 * Max is exclusive; i.e. default is (..., 0, 256)
	 */
	public static int safeGet(byte[] data, int ptr, int min, int max) {
		if (ptr < 0 || ptr >= data.length)
			return 0;
		int val = data[ptr] & 0xFF;
		if (val >= min && val < max)
			return val;
		val -= min;
		val %= max - min;
		val += min;
		return val;
	}

	/**
	 * Chemical reference
	 */
	public static void summarizeChemRef(VirtualCatalogue catalogue, StringBuilder builder, int chem) {
		String name = catalogue.findChemName(chem);
		if (name != null) {
			builder.append(name);
			builder.append(" ");
		}
		builder.append("[");
		builder.append(chem);
		builder.append("]");
	}
}
