/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.names;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import natsue.data.babel.PacketReader;

/**
 * Password hasher.
 */
public class PWHash {
	public static boolean verify(int uid, String hash, String password) {
		if (hash.startsWith("G1:")) {
			return hash.equals(hashG1(uid, password));
		} else if (hash.startsWith("DEV:")) {
			// For the dev instance ONLY. No part of Natsue should ever automatically write this.
			// Basically, this allows a developer to use sqlitebrowser to initialize the passwords of their accounts.
			return hash.equals("DEV:" + password);
		}
		return false;
	}

	/**
	 * Hash using the current scheme.
	 */
	public static String hash(int uid, String password) {
		return hashG1(uid, password);
	}

	/**
	 * Hash using scheme G1 (lightly salted SHA-256)
	 */
	public static String hashG1(int uid, String password) {
		byte[] data = ("PWHash_" + uid + "_" + password).getBytes(PacketReader.CHARSET);
		byte[] digest;
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(data);
			digest = md.digest();
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		return "G1:" + Base64.getEncoder().encodeToString(digest);
	}

	public static void main(String[] args) {
		System.out.println(hash(Integer.valueOf(args[0]), args[1]));
	}
}
