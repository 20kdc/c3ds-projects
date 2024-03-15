/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.caos;

import java.nio.charset.Charset;

import cdsp.common.data.bytestring.ByteSequence;
import cdsp.common.data.bytestring.ByteString;
import cdsp.common.data.bytestring.W1252Fixed;
import rals.code.CodeGenFeatureLevel;

/**
 * Utilities for writing CAOS.
 */
public class CAOSUtils {
	/**
	 * Character set for a standard copy of Creatures 3 or Docking Station.
	 */
	public static final Charset CAOS_CHARSET = W1252Fixed.INSTANCE;

	/**
	 * Converts a VA index into the VA name.
	 */
	public static String vaToString(String pfx, int va) {
		String res = Integer.toString(va);
		if (res.length() == 1)
			return pfx + "0" + res;
		return pfx + res;
	}

	/**
	 * Converts a VA index into the VA name.
	 */
	public static String vaToString(int va) {
		// [CAOS]
		return vaToString("va", va);
	}

	/**
	 * Checks if a string is inbounds.
	 */
	public static void checkStringCAOSConstant(ByteSequence value, CodeGenFeatureLevel codeGen) {
		if (codeGen.lexerConstantStringLimit != -1)
			if (value.length() > codeGen.lexerConstantStringLimit)
				throw new ConstantTooLargeForVMException();
	}

	/**
	 * Converts into a CAOS constant.
	 * Note the code gen feature level - this controls a check that prevents crashes!
	 */
	public static ByteString.Builder stringIntoCAOSConstant(ByteSequence value, CodeGenFeatureLevel codeGen) {
		ByteString.Builder res = new ByteString.Builder(value.length() + 2);
		stringIntoCAOSConstant(res, value, codeGen);
		return res;
	}

	/**
	 * Converts into a CAOS constant.
	 * Note the code gen feature level - this controls a check that prevents crashes!
	 */
	public static void stringIntoCAOSConstant(ByteString.Builder res, ByteSequence value, CodeGenFeatureLevel codeGen) {
		checkStringCAOSConstant(value, codeGen);
		byte[] valueBytes = value.getBytes();
		res.write('"');
		for (byte c : valueBytes) {
			if ((c == '\\') || (c == '\"')) {
				res.write('\\');
				res.write(c);
			} else if (c == '\r') {
				res.write('\\');
				res.write('r');
			} else if (c == '\n') {
				res.write('\\');
				res.write('n');
			} else if (c == '\t') {
				res.write('\\');
				res.write('t');
			} else if (c == 0) {
				res.write('\\');
				res.write('0');
			} else {
				res.write(c);
			}
		}
		res.write('"');
	}

	/**
	 * Unescapes the result of the OUTX command.
	 */
	public static String unescapeOUTX(String string) {
		StringBuilder sb = new StringBuilder();
		if (!string.startsWith("\""))
			throw new RuntimeException("unescapeOUTX: " + string);
		if (!string.endsWith("\""))
			throw new RuntimeException("unescapeOUTX: " + string);
		string = string.substring(1, string.length() - 1);
		boolean isEscaping = false;
		for (char c : string.toCharArray()) {
			if (isEscaping) {
				char conv = c;
				if (c == 'n') {
					conv = '\n';
				} else if (c == 'r') {
					conv = '\r';
				} else if (c == '0') {
					conv = 0;
				} else if (c == 't') {
					conv = '\t';
				}
				sb.append(conv);
				isEscaping = false;
			} else if (c == '\\') {
				isEscaping = true;
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	/**
	 * Indicates the constant needs to be broken down.
	 */
	@SuppressWarnings("serial")
	public static class ConstantTooLargeForVMException extends RuntimeException {
		public ConstantTooLargeForVMException() {
			super("A constant string was too large for the target CAOS VM.");
		}
	}
}
