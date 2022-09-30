/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.expr;

import rals.code.ScopeContext;
import rals.types.Classifier;
import rals.types.RALType;
import rals.types.TypeSystem;

/**
 * This appears in the expression tree when an ID hasn't been resolved to a specific meaning yet.
 */
public class RALAmbiguousID implements RALExprUR {
	public final TypeSystem typeSystem;
	public final String text;

	public RALAmbiguousID(TypeSystem ts, String txt) {
		typeSystem = ts;
		text = txt;
	}

	@Override
	public String toString() {
		return "(id " + text + ")";
	}

	@Override
	public RALExpr resolve(ScopeContext context) {
		if (context != null) {
			RALExpr re = context.scopedVariables.get(text);
			if (re != null)
				return re;
		}
		RALConstant rc = typeSystem.namedConstants.get(text);
		if (rc != null)
			return rc;
		RALType.AgentClassifier maybeClassifier = typeSystem.tryGetAsClassifier(text);
		if (maybeClassifier != null) {
			Classifier cl = maybeClassifier.classifier;
			return RALExprGroup.of(
				new RALConstant.Int(typeSystem, cl.family),
				new RALConstant.Int(typeSystem, cl.genus),
				new RALConstant.Int(typeSystem, cl.species)
			).resolve(context);
		}
		throw new RuntimeException("Unknown ID " + text);
	}
}
