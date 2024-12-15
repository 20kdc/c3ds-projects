/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package cdsp.common.data.skeleton;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;

/**
 * ATT file data.
 */
public class ATTFile {
	private final int[][] directions;

	public ATTFile(File f) throws IOException {
		this(new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.ISO_8859_1)));
	}

	public ATTFile(BufferedReader br) throws IOException {
		LinkedList<int[]> framesList = new LinkedList<>();
		while (true) {
			String line = br.readLine();
			if (line == null)
				break;
			String[] entries = line.split(" ");
			LinkedList<Integer> entryData = new LinkedList<>();
			for (String ent : entries) {
				try {
					entryData.add(Integer.valueOf(ent));
				} catch (Exception ex) {
					// ...
				}
			}
			int[] res = new int[entryData.size()];
			int idx = 0;
			for (Integer v : entryData)
				res[idx++] = v;
			framesList.add(res);
		}
		directions = framesList.toArray(new int[0][]);
	}

	private int getValue(int direction, int idx) {
		if (direction < 0 || direction >= directions.length)
			return 0;
		int[] data = directions[direction];
		if (idx < 0 || idx >= data.length)
			return 0;
		return data[idx];
	}

	public int getX(int direction, int joint) {
		return getValue(direction, joint * 2);
	}

	public int getY(int direction, int joint) {
		return getValue(direction, (joint * 2) + 1);
	}
}
