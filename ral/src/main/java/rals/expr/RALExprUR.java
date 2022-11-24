/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.expr;

import rals.code.ScopeContext;
import rals.types.TypeSystem;

/**
 * Unresolved expression. Created to ensure expressions get resolved.
 */
public interface RALExprUR {
	/**
	 * Attempts to resolve as a const, or returns null otherwise.
	 */
	default RALConstant resolveConst(TypeSystem ts) {
		return null;
	}

	/**
	 * Must resolve.
	 * Will fill ScopeContext with stuff that might be important for the target expression to be runnable.
	 * Note that this doesn't generate code - it just gets all the variables into place.
	 */
	default RALExprSlice resolve(ScopeContext scope) {
		RALConstant rc = resolveConst(scope.world.types);
		if (rc != null)
			return rc;
		RALExprSlice res = resolveInner(scope);
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
	 * Used for assignment statements because of how writing works.
	 * THIS NEEDS TO BE REMOVED IF AT ALL POSSIBLE post sliceapi
	 */
	default RALExprUR[] decomposite() {
		return new RALExprUR[] {this};
	}
}
