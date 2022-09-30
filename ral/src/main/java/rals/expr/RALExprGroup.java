/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.expr;

import java.io.StringWriter;
import java.util.LinkedList;

import rals.code.ScopeContext;
import rals.code.ScriptContext;
import rals.types.RALType;

/**
 * Expression group - a list of expressions.
 */
public class RALExprGroup implements RALExprUR {
	public final RALExprUR[] contents;

	private RALExprGroup(RALExprUR... c) {
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
		return new RALExprGroup(c);
	}

	@Override
	public RALExpr resolve(ScopeContext context) {
		RALExpr[] res = new RALExpr[contents.length];
		for (int i = 0; i < res.length; i++)
			res[i] = contents[i].resolve(context);
		return new Resolved(res);
	}

	@Override
	public RALExprUR[] decomposite() {
		return contents;
	}

	public static class Resolved implements RALExpr {
		public final RALExpr[] contents;

		public Resolved(RALExpr[] c) {
			contents = c;
		}
	
		@Override
		public RALType[] outTypes(ScriptContext context) {
			RALType[][] collection = new RALType[contents.length][];
			int total = 0;
			for (int i = 0; i < contents.length; i++) {
				collection[i] = contents[i].outTypes(context);
				total += collection[i].length;
			}
			RALType[] res = new RALType[total];
			int ptr = 0;
			for (int i = 0; i < contents.length; i++) {
				System.arraycopy(collection[i], 0, res, ptr, collection[i].length);
				ptr += collection[i].length;
			}
			return res;
		}
	
		@Override
		public void outCompile(StringBuilder writer, RALExpr[] out, ScriptContext context) {
			int ptr = 0;
			for (int i = 0; i < contents.length; i++) {
				int count = contents[i].outTypes(context).length;
				RALExpr[] slice = new RALExpr[count];
				System.arraycopy(out, ptr, slice, 0, count);
				contents[i].outCompile(writer, slice, context);
				ptr += count;
			}
		}
	
		@Override
		public RALType inType(ScriptContext context) {
			throw new RuntimeException("Not writable");
		}
	
		@Override
		public void inCompile(StringBuilder writer, String input, RALType inputExactType, ScriptContext context) {
			throw new RuntimeException("Not writable");
		}
	}
}
