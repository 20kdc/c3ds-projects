/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.expr;

import rals.code.*;
import rals.diag.SrcRange;
import rals.lex.Token;
import rals.types.*;

/**
 * This appears in the expression tree when an ID hasn't been resolved to a specific meaning yet.
 */
public class RALAmbiguousID implements RALExprUR {
	public final TypeSystem typeSystem;
	public final String text;
	// OPTIONAL, as these can be synthesized.
	public final Token.ID textToken;
	public final SrcRange extent;

	public RALAmbiguousID(SrcRange ex, TypeSystem ts, String txt) {
		extent = ex;
		typeSystem = ts;
		text = txt;
		textToken = null;
	}

	public RALAmbiguousID(SrcRange ex, TypeSystem ts, Token.ID txt) {
		extent = ex;
		typeSystem = ts;
		text = txt.text;
		textToken = txt;
	}

	@Override
	public String toString() {
		return "(id " + text + ")";
	}

	@Override
	public RALConstant resolveConst(TypeSystem ts) {
		RALConstant rc = typeSystem.namedConstants.get(text);
		if (rc != null)
			return rc;
		return null;
	}

	@Override
	public RALExprSlice resolveInner(ScopeContext context) {
		// Constants go first for consistency with the const resolver.
		RALConstant rc = typeSystem.namedConstants.get(text);
		if (rc != null)
			return rc;
		ScopeContext.LVar re = context.scopedVariables.get(text);
		if (re != null)
			return re.content;
		RALType.AgentClassifier maybeClassifier = typeSystem.tryGetAsClassifier(text);
		if (maybeClassifier != null) {
			Classifier cl = maybeClassifier.classifier;
			return RALExprGroupUR.of(
				new RALConstant.Int(typeSystem, cl.family),
				new RALConstant.Int(typeSystem, cl.genus),
				new RALConstant.Int(typeSystem, cl.species)
			).resolve(context);
		}
		context.world.diags.error(extent, "Unknown ID " + text);
		return RALExprSlice.EMPTY;
	}
}
