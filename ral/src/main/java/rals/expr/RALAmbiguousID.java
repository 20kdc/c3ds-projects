/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.expr;

import java.util.Set;

import rals.code.*;
import rals.diag.SrcRange;
import rals.lex.Token;
import rals.types.*;

/**
 * This appears in the expression tree when an ID hasn't been resolved to a specific meaning yet.
 */
public class RALAmbiguousID implements RALExprUR {
	public final String text;
	// OPTIONAL, as these can be synthesized.
	public final Token.ID textToken;
	public final SrcRange extent;

	public RALAmbiguousID(SrcRange ex, String txt) {
		extent = ex;
		text = txt;
		textToken = null;
	}

	public RALAmbiguousID(SrcRange ex, Token.ID txt) {
		extent = ex;
		text = txt.text;
		textToken = txt;
	}

	@Override
	public String toString() {
		return "(id " + text + ")";
	}

	@Override
	public RALConstant resolveConst(TypeSystem ts, Set<String> scopedVariables) {
		if (scopedVariables.contains(text))
			return null;
		return ts.namedConstants.get(text);
	}

	@Override
	public RALExprSlice resolveInner(ScopeContext context) {
		TypeSystem typeSystem = context.world.types;
		ScopeContext.LVar re = context.scopedVariables.get(text);
		if (re != null)
			return re.content;
		RALConstant rc = typeSystem.namedConstants.get(text);
		if (rc != null)
			return rc;
		RALType.AgentClassifier maybeClassifier = typeSystem.tryGetAsClassifier(text);
		if (maybeClassifier != null) {
			Classifier cl = maybeClassifier.classifier;
			return RALExprGroupUR.of(
				new RALConstant.Int(typeSystem, cl.family),
				new RALConstant.Int(typeSystem, cl.genus),
				new RALConstant.Int(typeSystem, cl.species)
			).resolve(context);
		}
		if (extent != null)
			context.world.diags.pushFrame(extent);
		context.world.diags.error("Unknown ID " + text);
		if (extent != null)
			context.world.diags.popFrame(extent);
		return RALExprSlice.EMPTY;
	}
}
