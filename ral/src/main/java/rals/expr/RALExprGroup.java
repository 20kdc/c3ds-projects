/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.expr;

import rals.code.ScopeContext;
import rals.types.RALType;

/**
 * Expression group - a list of expressions.
 */
public class RALExprGroup implements RALExprUR {
	public final RALExprUR[] contents;

	public RALExprGroup(RALExprUR... c) {
		contents = c;
	}

	@Override
	public RALExpr resolve(ScopeContext context) {
		RALExpr[] res = new RALExpr[contents.length];
		for (int i = 0; i < res.length; i++)
			res[i] = contents[i].resolve(context);
		return new Resolved(res);
	}

	public static class Resolved implements RALExpr {
		public final RALExpr[] contents;

		public Resolved(RALExpr[] c) {
			contents = c;
		}

		private RALType[] inOutTypes(ScopeContext context, boolean in) {
			RALType[][] collection = new RALType[contents.length][];
			int total = 0;
			for (int i = 0; i < contents.length; i++) {
				collection[i] = in ? contents[i].inTypes(context) : contents[i].outTypes(context);
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
		public RALType[] outTypes(ScopeContext context) {
			return inOutTypes(context, false);
		}
	
		@Override
		public void outCompile(StringBuilder writer, RALExpr[] out, ScopeContext context) {
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
		public RALType[] inTypes(ScopeContext context) {
			return inOutTypes(context, true);
		}
	
		@Override
		public void inCompile(StringBuilder writer, String[] input, RALType[] inputExactType, ScopeContext context) {
			int ptr = 0;
			for (int i = 0; i < contents.length; i++) {
				int count = contents[i].inTypes(context).length;
				String[] sliceS = new String[count];
				RALType[] sliceT = new RALType[count];
				System.arraycopy(input, ptr, sliceS, 0, count);
				System.arraycopy(inputExactType, ptr, sliceT, 0, count);
				contents[i].inCompile(writer, sliceS, sliceT, context);
				ptr += count;
			}
		}
	}
}
