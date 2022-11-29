/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.expr;

import java.util.LinkedList;

import rals.code.*;
import rals.lex.DefInfo;
import rals.stmt.RALBlock;
import rals.stmt.RALLetStatement;
import rals.stmt.RALModAssignStatement;
import rals.stmt.RALStatement;
import rals.types.*;

/**
 * Binary operators!
 * Note that if you were looking for something that works on booleans, you're looking for RALCondLogOp.
 * This isn't that.
 */
public class RALChainOp implements RALExprUR {
	public final RALExprUR[] elements;
	public final RALModAssignStatement.Op op;
	public final DefInfo.At defInfo;

	public RALChainOp(DefInfo.At ex, RALModAssignStatement.Op o, RALExprUR[] elm) {
		op = o;
		elements = elm;
		defInfo = ex;
	}

	public static RALExprUR of(DefInfo.At di, RALExprUR l, RALModAssignStatement.Op thisOp, RALExprUR r) {
		LinkedList<RALExprUR> total = new LinkedList<>();
		boolean handledL = false;
		if (l instanceof RALChainOp) {
			RALChainOp leftCO = (RALChainOp) l;
			if (leftCO.op == thisOp) {
				for (RALExprUR elm : leftCO.elements)
					total.add(elm);
				handledL = true;
			}
		}
		if (!handledL)
			total.add(l);
		total.add(r);
		return new RALChainOp(di, thisOp, total.toArray(new RALExprUR[0]));
	}

	@Override
	public RALConstant resolveConst(TypeSystem ts) {
		RALConstant res = null;
		for (int i = 0; i < elements.length; i++) {
			RALConstant c = elements[i].resolveConst(ts);
			if (c == null)
				return null;
			if (i == 0) {
				res = c;
			} else {
				res = op.stepConst(ts, res, c);
			}
		}
		return res;
	}

	@Override
	public RALExprSlice resolveInner(ScopeContext scope) {
		// pass 1: collate slices
		RALExprSlice[] allArgSlices = new RALExprSlice[elements.length];
		RALType[] allArgTypes = new RALType[elements.length];
		for (int i = 0; i < elements.length; i++) {
			RALExprSlice slice = elements[i].resolve(scope);
			RALType t = slice.assert1ReadType();
			// type checks themselves will be done during the main build
			allArgSlices[i] = slice;
			allArgTypes[i] = t;
		}
		// pass 1.5: create initial let statement
		LinkedList<RALStatement> stmts = new LinkedList<>();
		IVAHandle vh = new IVAHandle() {
			@Override
			public String toString() {
				return "RALChainOp temporary";
			}
		};
		RALType currentLeftType = allArgTypes[0];
		stmts.add(new RALLetStatement.Resolved(defInfo.srcRange, new String[] {"__ralChainOpCarrier"}, new RALVarVA[] {new RALVarVA(vh, currentLeftType)}, allArgSlices[0]));
		// pass 2: create statements and calculate final type
		for (int i = 1; i < elements.length; i++) {
			// Note that we ignore any type check errors by swapping out the RALVarVA at every step.
			// This is basically a really fancy cast.
			RALVarVA currentVar = new RALVarVA(vh, currentLeftType);
			stmts.add(new RALModAssignStatement.Resolved(defInfo.srcRange, currentVar, allArgSlices[i], op, scope.world.types));
			currentLeftType = op.stepType(scope.world.types, currentLeftType, allArgTypes[i]);
		}
		RALStatement rbx = new RALBlock.Resolved(defInfo.srcRange, stmts, false);
		return new RALStmtExpr.Resolved(rbx, new RALVarVA(vh, currentLeftType));
	}
}
