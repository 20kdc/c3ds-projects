/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.code;

import java.util.HashMap;

import rals.diag.DiagRecorder;
import rals.expr.*;
import rals.types.TypeSystem;

/**
 * A set of macro definitions.
 */
public class MacroDefSet implements RALCallable {
	public HashMap<Integer, RALCallable> map = new HashMap<>();
	public final String name;

	public MacroDefSet(String n) {
		name = n;
	}

	@Override
	public void precompile(TypeSystem ts, ScriptsUR source, DiagRecorder diags, Scripts target) {
		for (RALCallable rc : map.values())
			rc.precompile(ts, source, diags, target);
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
		map.put(count, c);
	}
}
