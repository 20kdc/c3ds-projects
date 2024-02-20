/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.data;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import natsue.server.database.UnixTime;

/**
 * 2FA security???
 */
public class TOTP {
	private static final char[] BASE32_CHARS = ("ABCDEFGHIJKLMNOPQRSTUVWXYZ234567").toCharArray();

	/**
	 * Decodes a base32 character.
	 */
	public static int decodeBase32Char(char chr) throws InvalidTOTPKeyException {
		if (chr >= 'a' && chr <= 'z')
			return chr - 'a';
		if (chr >= 'A' && chr <= 'Z')
			return chr - 'A';
		if (chr >= '2' && chr <= '7')
			return (chr - '2') + 26;
		throw new InvalidTOTPKeyException("Invalid base32 character");
	}

	/**
	 * Decodes a base32 8-character segment into 5 bytes.
	 */
	public static void decodeBase32Segment(char[] inChars, int inOffset, byte[] outBytes, int outOffset) throws InvalidTOTPKeyException {
		// shift in
		long total = 0;
		for (int i = 0; i < 8; i++) {
			total = total << 5;
			total |= decodeBase32Char(inChars[inOffset++]);
		}
		// shift out
		for (int i = 0; i < 5; i++) {
			outBytes[outOffset++] = (byte) (total >> 32);
			total = total << 8;
		}
	}

	/**
	 * Encodes a base32 8-character segment from 5 bytes.
	 */
	public static void encodeBase32Segment(byte[] inBytes, int inOffset, char[] outChars, int outOffset) {
		// shift in
		long total = 0;
		for (int i = 0; i < 5; i++) {
			total = total << 8;
			total |= inBytes[inOffset++] & 0xFF;
		}
		// shift out
		for (int i = 0; i < 8; i++) {
			outChars[outOffset + 7 - i] = BASE32_CHARS[(int) (total & 0x1F)];
			total = total >> 5;
		}
	}

	/**
	 * Decodes base32.
	 */
	public static byte[] decodeBase32(String base32) throws InvalidTOTPKeyException {
		char[] chr = base32.toCharArray();
		return decodeBase32(chr, 0, chr.length);
	}

	/**
	 * Decodes base32.
	 */
	public static byte[] decodeBase32(char[] base32, int inOffset, int inLength) throws InvalidTOTPKeyException {
		byte[] res = new byte[(inLength / 8) * 5];
		int outOffset = 0;
		while (inLength >= 8) {
			decodeBase32Segment(base32, inOffset, res, outOffset);
			inOffset += 8;
			outOffset += 5;
			inLength -= 8;
		}
		if (inLength != 0)
			throw new InvalidTOTPKeyException("Base32 string not a multiple of 8 characters");
		return res;
	}

	public static char[] encodeBase32(byte[] data) throws InvalidTOTPKeyException {
		return encodeBase32(data, 0, data.length);
	}

	public static char[] encodeBase32(byte[] data, int inOffset, int inLength) throws InvalidTOTPKeyException {
		char[] res = new char[(inLength / 5) * 8];
		int outOffset = 0;
		while (inLength >= 5) {
			encodeBase32Segment(data, inOffset, res, outOffset);
			inOffset += 5;
			outOffset += 8;
			inLength -= 5;
		}
		if (inLength != 0)
			throw new InvalidTOTPKeyException("Data not a multiple of 5 bytes");
		return res;
	}

	/**
	 * "dynamic truncation" and all that
	 */
	public static int hashToDigits(byte... data) {
		int ofs = data[19] & 0xF;
		int dbc2 = ((data[ofs] & 0x7F) << 24) | ((data[ofs + 1] & 0xFF) << 16) | ((data[ofs + 2] & 0xFF) << 8) | (data[ofs + 3] & 0xFF);
		return dbc2 % 1000000;
	}

	/**
	 * Calculates the 6-digit result for the given key and counter value.
	 */
	public static int calculate(byte[] key, long i) {
		try {
			Mac mac = Mac.getInstance("HmacSHA1");
			mac.init(new SecretKeySpec(key, "HmacSHA1"));
			ByteBuffer bb = ByteBuffer.allocate(8);
			bb.order(ByteOrder.BIG_ENDIAN);
			bb.putLong(0, i);
			byte[] res = mac.doFinal(bb.array());
			return hashToDigits(res);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Verifies a set of digits against a key.
	 * This function is time-dependent.
	 */
	public static boolean verify(byte[] key, int i) {
		long ref = UnixTime.get() / 30;
		int ca = calculate(key, ref - 1);
		int cb = calculate(key, ref);
		// buggy implementation, desynced clock, etc.
		int cc = calculate(key, ref + 1);
		if (i == ca)
			return true;
		if (i == cb)
			return true;
		if (i == cc)
			return true;
		return false;
	}

	@SuppressWarnings("serial")
	public static class InvalidTOTPKeyException extends Exception {
		public InvalidTOTPKeyException(String reason) {
			super(reason);
		}
	}
}
