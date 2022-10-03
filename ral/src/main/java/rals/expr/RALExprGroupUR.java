/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.expr;

import java.util.LinkedList;

import rals.code.*;

/**
 * Expression group - a list of expressions.
 */
public class RALExprGroupUR implements RALExprUR {
	public final RALExprUR[] contents;

	private RALExprGroupUR(RALExprUR... c) {
		if (c.length == 1)
			throw new RuntimeException("Don't make these for single elements.");
		contents = c;
	}

	public static RALExprUR of(RALExprUR... c) {
		// decompose here - this is so that further decomposition acts right
		LinkedList<RALExprUR> decomposed = new LinkedList<>();
		for (RALExprUR ur : c)
			for (RALExprUR e : ur.decomposite())
				decomposed.add(e);
		c = decomposed.toArray(new RALExprUR[0]);
		// now that it's decomposed...
		if (c.length == 1)
			return c[0];
		return new RALExprGroupUR(c);
	}

	@Override
	public RALExprSlice resolveInner(ScopeContext context) {
		RALExprSlice res = RALExprSlice.EMPTY;
		for (int i = 0; i < contents.length; i++)
			res = RALExprSlice.concat(res, contents[i].resolve(context));
		return res;
	}

	@Override
	public RALExprUR[] decomposite() {
		return contents;
	}
}
