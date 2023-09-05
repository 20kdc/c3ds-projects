/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package cdsp.common.data.pray;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;

import cdsp.common.data.IOUtils;

/**
 * Going all-out on this...
 */
public class PRAYTags {
	public final Charset charset;

	public final HashMap<String, Integer> intMap = new HashMap<String, Integer>();
	public final HashMap<String, String> strMap = new HashMap<String, String>();

	public PRAYTags(Charset charset) {
		this.charset = charset;
	}

	public void read(byte[] block) {
		ByteBuffer bb = IOUtils.wrapLE(block);
		int intValNo = bb.getInt();
		for (int i = 0; i < intValNo; i++) {
			String key = IOUtils.getString(bb, charset);
			int val = bb.getInt();
			intMap.put(key, val);
		}
		int strValNo = bb.getInt();
		for (int i = 0; i < strValNo; i++) {
			String key = IOUtils.getString(bb, charset);
			strMap.put(key, IOUtils.getString(bb, charset));
		}
	}

	public byte[] toByteArray() {
		LinkedList<byte[]> intKeyByteArrays = new LinkedList<byte[]>();
		LinkedList<Integer> intValues = new LinkedList<Integer>();
		LinkedList<byte[]> stringByteArrays = new LinkedList<byte[]>();
		int totalSize = 8;
		for (Entry<String, Integer> ent : intMap.entrySet()) {
			byte[] k = ent.getKey().getBytes(charset);
			intKeyByteArrays.add(k);
			intValues.add(ent.getValue());
			totalSize += 8 + k.length;
		}
		for (Entry<String, String> ent : strMap.entrySet()) {
			byte[] k = ent.getKey().getBytes(charset);
			byte[] v = ent.getValue().getBytes(charset);
			stringByteArrays.add(k);
			stringByteArrays.add(v);
			totalSize += 8 + k.length + v.length;
		}

		ByteBuffer res = IOUtils.newBuffer(totalSize);
		Iterator<byte[]> iK;
		Iterator<Integer> iV;

		res.putInt(intValues.size());
		iK = intKeyByteArrays.iterator();
		iV = intValues.iterator();
		while (iK.hasNext()) {
			byte[] key = iK.next();
			int val = iV.next();
			res.putInt(key.length);
			res.put(key);
			res.putInt(val);
		}

		res.putInt(stringByteArrays.size() / 2);
		iK = stringByteArrays.iterator();
		while (iK.hasNext()) {
			byte[] key = iK.next();
			res.putInt(key.length);
			res.put(key);
		}

		return res.array();
	}
}
