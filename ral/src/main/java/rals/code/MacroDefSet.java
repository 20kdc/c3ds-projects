/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.code;

import java.util.HashMap;

import rals.expr.*;
import rals.lex.DefInfo;

/**
 * A set of macro definitions.
 */
public class MacroDefSet implements RALCallable {
	public HashMap<Integer, RALCallable> map = new HashMap<>();
	public int lowestCount = -1;
	public final String name;

	public MacroDefSet(String n) {
		name = n;
	}

	@Override
	public DefInfo getDefInfo() {
		// return the lowest-arged one we find
		RALCallable check = map.get(lowestCount);
		if (check != null)
			return check.getDefInfo();
		return null;
	}

	@Override
	public void precompile(UnresolvedWorld world) {
		for (RALCallable rc : map.values())
			rc.precompile(world);
	}

	@Override
	public RALExprSlice instance(RALExprSlice args, ScopeContext sc) {
		RALCallable res = map.get(args.length);
		if (res == null)
			throw new RuntimeException(name + " doesn't have " + args.length + "-parameter variant");
		return res.instance(args, sc);
	}

	public void addMacro(int count, RALCallable c) {
		if (map.containsKey(count))
			throw new RuntimeException(name + " already has " + count + "-arg variant");
		if ((lowestCount == -1) || (lowestCount > count))
			lowestCount = count;
		map.put(count, c);
	}
}
