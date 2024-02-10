/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.expr;

import java.util.Set;

import rals.code.ScopeContext;
import rals.types.TypeSystem;

/**
 * Unresolved expression. Created to ensure expressions get resolved.
 */
public interface RALExprUR {
	/**
	 * Attempts to resolve as a const, or returns null otherwise.
	 */
	default RALConstant resolveConst(TypeSystem ts, Set<String> scopedVariables) {
		return null;
	}

	/**
	 * Must resolve.
	 * Will fill ScopeContext with stuff that might be important for the target expression to be runnable.
	 * Note that this doesn't generate code - it just gets all the variables into place.
	 */
	default RALExprSlice resolve(ScopeContext scope) {
		RALConstant rc = resolveConst(scope.world.types, scope.scopedVariables.keySet());
		if (rc != null) {
			scope.world.hcm.onResolveExpression(this, rc);
			return rc;
		}
		RALExprSlice res = resolveInner(scope);
		scope.world.hcm.onResolveExpression(this, res);
		return res;
	}

	/**
	 * Must resolve.
	 * Will fill ScopeContext with stuff that might be important for the target expression to be runnable.
	 * Note that this doesn't generate code - it just gets all the variables into place.
	 * NOTE: Because RALExprUR is an interface, I can't stop this from being called, so just please don't?
	 * Only resolve is meant to call this.
	 */
	RALExprSlice resolveInner(ScopeContext scope);

	/**
	 * Decomposites expression groups.
	 * The plan was to remove this, but it's turned out to be rather useful in doing two things:
	 * 1. Keeping constants (byte strings!!!) sane
	 * 2. It's used by RALExprGroupUR to avoid nesting groups
	 */
	default RALExprUR[] decomposite() {
		return new RALExprUR[] {this};
	}
}
