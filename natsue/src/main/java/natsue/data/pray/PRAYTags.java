/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.data.pray;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;

import natsue.data.IOUtils;
import natsue.data.babel.PacketReader;

/**
 * Going all-out on this...
 */
public class PRAYTags {
	public final HashMap<String, Integer> intMap = new HashMap<String, Integer>();
	public final HashMap<String, String> strMap = new HashMap<String, String>();

	public void read(byte[] block) {
		ByteBuffer bb = PacketReader.wrapLE(block);
		int intValNo = bb.getInt();
		for (int i = 0; i < intValNo; i++) {
			String key = PacketReader.getString(bb);
			int val = bb.getInt();
			intMap.put(key, val);
		}
		int strValNo = bb.getInt();
		for (int i = 0; i < strValNo; i++) {
			String key = PacketReader.getString(bb);
			strMap.put(key, PacketReader.getString(bb));
		}
	}

	public byte[] toByteArray() {
		LinkedList<byte[]> intKeyByteArrays = new LinkedList<byte[]>();
		LinkedList<Integer> intValues = new LinkedList<Integer>();
		LinkedList<byte[]> stringByteArrays = new LinkedList<byte[]>();
		int totalSize = 8;
		for (Entry<String, Integer> ent : intMap.entrySet()) {
			byte[] k = ent.getKey().getBytes(PacketReader.CHARSET);
			intKeyByteArrays.add(k);
			intValues.add(ent.getValue());
			totalSize += 8 + k.length;
		}
		for (Entry<String, String> ent : strMap.entrySet()) {
			byte[] k = ent.getKey().getBytes(PacketReader.CHARSET);
			byte[] v = ent.getValue().getBytes(PacketReader.CHARSET);
			stringByteArrays.add(ent.getKey().getBytes(PacketReader.CHARSET));
			stringByteArrays.add(ent.getValue().getBytes(PacketReader.CHARSET));
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
