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
import rals.stmt.RALAssignStatement;
import rals.stmt.RALBlock;
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
		IEHHandle eh = new IEHHandle() {
			@Override
			public String toString() {
				return "RALChainOp target";
			}
		};
		// Create the InplaceRes. This is the body of the RALChainOp.
		InplaceRes ipr = createInplace(eh, scope);
		final RALSlot res = new RALSlot(ipr.finishedType, RALSlot.Perm.R);
		return new RALExprSlice(1) {
			@Override
			protected RALSlot slotInner(int index) {
				return res;
			}

			@Override
			protected void readCompileInner(RALExprSlice out, CompileContext context) {
				try (CompileContext cc2 = new CompileContext(context)) {
					// Create a temporary and run things through that.
					RALVarVA va = cc2.allocVA(ipr.finishedType, "RALChainOp temporary");
					cc2.heldExprHandles.put(eh, va);
					ipr.compute.compile(cc2.writer, cc2);
					va.readCompile(out, cc2);
				}
			}

			@Override
			protected void readInplaceCompileInner(RALVarVA[] out, CompileContext context) {
				try (CompileContext cc2 = new CompileContext(context)) {
					// Who needs temporaries?
					cc2.heldExprHandles.put(eh, out[0]);
					ipr.compute.compile(cc2.writer, cc2);
				}
			}
		};
	}

	private InplaceRes createInplace(IEHHandle handle, ScopeContext scope) {
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
		// pass 1.5: create initial assignment statement
		LinkedList<RALStatement> stmts = new LinkedList<>();
		RALType currentLeftType = allArgTypes[0];
		stmts.add(new RALAssignStatement.Resolved(defInfo.srcRange, new RALVarEH(handle, currentLeftType), allArgSlices[0]));
		// pass 2: create statements and calculate final type
		for (int i = 1; i < elements.length; i++) {
			// Note that we ignore any type check errors by swapping out the RALVarEH at every step.
			// This is basically a really fancy cast.
			RALVarEH currentVar = new RALVarEH(handle, currentLeftType);
			stmts.add(new RALModAssignStatement.Resolved(defInfo.srcRange, currentVar, allArgSlices[i], op, scope.world.types));
			currentLeftType = op.stepType(scope.world.types, currentLeftType, allArgTypes[i]);
		}
		return new InplaceRes(new RALBlock.Resolved(defInfo.srcRange, stmts, true), allArgTypes[0], currentLeftType);
	}

	private class InplaceRes {
		final RALStatement compute;
		//final RALType initialType;
		final RALType finishedType;
		InplaceRes(RALStatement stmt, RALType it, RALType ft) {
			compute = stmt;
			//initialType = it;
			finishedType = ft;
		}
	}
}
