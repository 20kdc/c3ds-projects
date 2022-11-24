/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.hcm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import rals.diag.SrcPosBase;
import rals.diag.SrcRange;

/**
 * Theoretically meant to be a map that's good for source range in case efficiency gains are needed in future.
 * Gained additional functionality to deal with "everything after this" sorta stuff (last token map, used as anchor for HCM)
 */
public class SrcPosMap<T> {
	// NOTE: DO NOT REMOVE ELEMENTS FROM THIS.
	// It will break consistency w/ lastLineInMap.
	private final HashMap<Integer, SrcLineMap> lineMap = new HashMap<>();
	private int lastLineInMap = -1;

	// End filler.
	// Basically, 0 through lastLineInMap inc. is mixed between null/content.
	// Everything after there is endFiller.
	private T endFiller;

	public void put(SrcRange range, T res) {
		put(range.start.line, range.start.character, range.end.line, range.end.character, res);
	}

	private SrcLineMap getLine(int l) {
		SrcLineMap slm = lineMap.get(l);
		if (slm == null) {
			slm = new SrcLineMap();
			lineMap.put(l, slm);
			if (lastLineInMap < l)
				lastLineInMap = l;
		}
		return slm;
	}

	public void put(int startLine, int startChar, int endLine, int endChar, T res) {
		if (startLine > endLine) {
			throw new RuntimeException("Invalid lines");
		} else if (startLine == endLine) {
			SrcLineMap slm = getLine(startLine);
			slm.put(startChar, endChar, res);
		} else {
			getLine(startLine).putUntilEnd(startChar, res);
			for (int i = startLine + 1; i < endLine; i++)
				getLine(i).reset(res);
			getLine(endLine).put(0, endChar, res);
		}
	}

	public void putUntilEnd(SrcPosBase sp, T res) {
		putUntilEnd(sp.line, sp.character, res);
	}
	public void putUntilEnd(int startLine, int startChar, T res) {
		getLine(startLine).putUntilEnd(startChar, res);
		for (Map.Entry<Integer, SrcLineMap> i : lineMap.entrySet())
			if (i.getKey() > startLine)
				i.getValue().reset(res);
		endFiller = res;
	}

	public T get(SrcPosBase sp) {
		return get(sp.line, sp.character);
	}

	public T get(int line, int character) {
		SrcLineMap slm = lineMap.get(line);
		if (slm == null) {
			if (line > lastLineInMap)
				return endFiller;
			return null;
		}
		return slm.get(character);
	}

	private class SrcLineMap {
		/**
		 * The last value in this list is considered canonical to the remainder of the line.
		 */
		private ArrayList<T> lineValues = new ArrayList<>();

		private SrcLineMap() {
			lineValues.add(null);
		}

		private void ensureIndexValid(int idx) {
			T uev = getUntilEndValue();
			while (lineValues.size() <= idx)
				lineValues.add(uev);
		}

		private T getUntilEndValue() {
			return lineValues.get(lineValues.size() - 1);
		}

		private void reset(T v) {
			lineValues.clear();
			lineValues.add(v);
		}

		private void putUntilEnd(int start, T v) {
			ensureIndexValid(start);
			for (int i = start; i < lineValues.size(); i++)
				lineValues.set(i, v);
		}

		private void put(int start, int end, T v) {
			ensureIndexValid(end + 1);
			for (int i = start; i < end; i++)
				lineValues.set(i, v);
		}

		private T get(int character) {
			if (character >= lineValues.size())
				return getUntilEndValue();
			return lineValues.get(character);
		}
	}
}
