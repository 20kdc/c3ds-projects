/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package natsue.server.packet;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;

/**
 * PEM loading routines.
 */
public class PEM {
	public static byte[][] fileIntoPEMBodies(File f) throws IOException {
		List<String> lines = Files.readAllLines(f.toPath());
		StringBuilder dataBuilder = new StringBuilder();
		LinkedList<byte[]> potential = new LinkedList<>();
		for (String s : lines) {
			if (s.startsWith("-")) {
				String res = dataBuilder.toString();
				// we have a data object?
				if (!res.equals(""))
					potential.add(Base64.getDecoder().decode(res));
				dataBuilder.setLength(0);
			} else {
				dataBuilder.append(s.trim());
			}
		}
		return potential.toArray(new byte[0][]);
	}
	public static byte[] fileIntoPEMBody(File f) throws IOException {
		byte[][] bodies = fileIntoPEMBodies(f);
		if (bodies.length != 1)
			throw new IOException("PEM file " + f + " has an inappropriate count of PEM bodies (" + bodies.length + ") for target use-case, are you sure you didn't get fullchain and privkey confused?");
		return bodies[0];
	}

	public static Key loadPrivKey(File f, String algorithm) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
		byte[] privKey = fileIntoPEMBody(f);
		PKCS8EncodedKeySpec key = new PKCS8EncodedKeySpec(privKey);
		// no way to figure it out from the PKCS8EncodedKeySpec which literally has the algorithm in it
		if (algorithm.equals("")) {
			// https://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#KeyFactory
			String[] algorithmsToTry = {"RSA", "DiffieHellman", "DSA", "EC", "RSASSA-PSS"};
			for (String alg : algorithmsToTry) {
				try {
					KeyFactory res = KeyFactory.getInstance(alg);
					return res.generatePrivate(key);
				} catch (Exception ex) {
					// avoid generating noise :(
					// it's not like you can't try the individual algorithm manually and get an error
					// ex.printStackTrace();
				}
			}
			throw new NoSuchAlgorithmException("Unable to guess algorithm. Specify manually via sslPrivKeyAlgorithm");
		}
		KeyFactory res = KeyFactory.getInstance(algorithm);
		return res.generatePrivate(key);
	}

	public static Certificate[] loadCertChain(File f) throws IOException, CertificateException {
		byte[][] bodies = fileIntoPEMBodies(f);
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		Certificate[] certs = new Certificate[bodies.length];
		for (int i = 0; i < bodies.length; i++)
			certs[i] = cf.generateCertificate(new ByteArrayInputStream(bodies[i]));
		return certs;
	}
}
