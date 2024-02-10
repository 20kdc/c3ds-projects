/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.types;

import java.util.LinkedList;

import rals.code.MacroArgNameless;
import rals.expr.RALSlot.Perm;

/**
 * Used as a hashmap key.
 */
public final class RALLambdaSignature {
	public final LinkedList<RALType> rets = new LinkedList<>();
	public final LinkedList<RALType> args = new LinkedList<>();
	public final LinkedList<Perm> argsInline = new LinkedList<>();

	public RALLambdaSignature(Iterable<RALType> rets, Iterable<MacroArgNameless> ma) {
		for (RALType rt : rets) {
			this.rets.add(rt);
		}
		for (MacroArgNameless n : ma) {
			this.args.add(n.type);
			this.argsInline.add(n.isInline);
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof RALLambdaSignature) {
			return ((RALLambdaSignature) obj).rets.equals(rets) && ((RALLambdaSignature) obj).args.equals(args) && ((RALLambdaSignature) obj).argsInline.equals(argsInline);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return rets.hashCode() ^ args.hashCode() ^ argsInline.hashCode();
	}
}
