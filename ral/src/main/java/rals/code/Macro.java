/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.code;

import rals.diag.DiagRecorder;
import rals.diag.SrcRange;
import rals.expr.*;
import rals.lex.DefInfo;
import rals.types.*;

/**
 * Macros are used to replicate functions because CAOS doesn't have a proper version of those.
 */
public class Macro implements RALCallable {
	public final DefInfo.At defInfo;
	public final String name;
	public final MacroArg[] args;
	public final RALExprUR code;
	// precompilation state
	public RALExprSlice precompiledCode;
	public boolean isBeingPrecompiled;

	public Macro(DefInfo.At di, String n, MacroArg[] a, RALExprUR c) {
		defInfo = di;
		name = n;
		args = a;
		code = c;
	}

	@Override
	public DefInfo getDefInfo() {
		return defInfo;
	}

	@Override
	public void precompile(UnresolvedWorld world) {
		if (precompiledCode != null)
			return;
		if (isBeingPrecompiled)
			throw new RuntimeException("Recursive macro compilation @ " + name + "#" + args.length);
		isBeingPrecompiled = true;

		TypeSystem ts = world.types;
		ScriptContext msContext = new ScriptContext(world, ts.gAgentNullable, ts.gAny, ts.gAny, ts.gAny);
		ScopeContext scContext = new ScopeContext(msContext);
		for (MacroArg arg : args)
			scContext.setLoc(arg.name, defInfo, new RALVarEH(arg, arg.type));

		world.diags.pushFrame(defInfo.srcRange);
		try {
			world.hcm.resolvePre(defInfo.srcRange, scContext);
			precompiledCode = code.resolve(scContext);
			world.hcm.resolvePost(defInfo.srcRange, scContext);
		} catch (Exception ex) {
			world.diags.error("failed resolving: ", ex);
			precompiledCode = new RALErrorExpr("macro " + name + "#" + args.length + " failed to compile");
		}
		world.diags.popFrame(defInfo.srcRange);
	}

	@Override
	public RALExprSlice instance(final RALExprSlice a, ScopeContext sc) {
		if (a.length != args.length)
			throw new RuntimeException("Macro " + name + " called with " + a.length + " args, not " + args.length);

		boolean[] inline = new boolean[args.length];
		String[] names = new String[args.length];
		for (int i = 0; i < args.length; i++) {
			inline[i] = args[i].isInline != null;
			names[i] = args[i].name;
			// Typecheck
			RALSlot argSlot = a.slot(i);
			RALSlot.Perm wantedPerms = args[i].computeRequiredPerms();
			argSlot.perms.require(this, wantedPerms);
			if (wantedPerms.read)
				argSlot.type.assertImpCast(args[i].type);
			if (wantedPerms.write)
				args[i].type.assertImpCast(argSlot.type);
		}

		VarCacher vc = new VarCacher(a, inline, names);

		// ensure compiled, then resolve with that code
		precompile(sc.world);
		return new Resolved(name, defInfo.srcRange, vc, precompiledCode, args);
	}

	public static final class Resolved extends RALExprSlice {
		private final VarCacher vc;
		public final RALExprSlice innards;
		public final String macroName;
		public final SrcRange macroExt;
		public final MacroArg[] macroArgs;

		public Resolved(String mn, SrcRange me, VarCacher vc, RALExprSlice innards, MacroArg[] args) {
			super(innards.length);
			macroName = mn;
			macroExt = me;
			this.vc = vc;
			this.innards = innards;
			macroArgs = args;
		}

		@Override
		protected RALExprSlice sliceInner(int tB, int tL) {
			return new Resolved(macroName, macroExt, vc, innards.slice(tB, tL), macroArgs);
		}

		@Override
		protected RALExprSlice tryConcatWithInner(RALExprSlice b) {
			if (b instanceof Resolved) {
				if (((Resolved) b).vc == vc) {
					// this is the same instance, so share!
					return new Resolved(macroName, macroExt, vc, RALExprSlice.concat(innards, ((Resolved) b).innards), macroArgs);
				}
			}
			return super.tryConcatWithInner(b);
		}

		public void installMacroArgs(CompileContextNW c2) {
			for (int i = 0; i < macroArgs.length; i++)
				c2.heldExprHandles.put(macroArgs[i], vc.finishedOutput.slice(i, 1));
		}

		@Override
		protected RALSlot slotInner(int index) {
			return innards.slot(index);
		}

		@Override
		public void writeCompileInner(int index, String input, RALType.Major inputExactType, CompileContext context) {
			try (DiagRecorder.Scope ds = context.diags.newScope(macroExt)) {
				try (CompileContext c2 = new CompileContext(context)) {
					vc.writeCacheCode(c2);
					installMacroArgs(c2);
					innards.writeCompile(index, input, inputExactType, c2);
				}
			}
		}

		@Override
		public void readCompileInner(RALExprSlice out, CompileContext context) {
			try (DiagRecorder.Scope ds = context.diags.newScope(macroExt)) {
				try (CompileContext c2 = new CompileContext(context)) {
					vc.writeCacheCode(c2);
					installMacroArgs(c2);
					innards.readCompile(out, c2);
				}
			}
		}

		@Override
		protected void readInplaceCompileInner(RALVarVA[] out, CompileContext context) {
			try (DiagRecorder.Scope ds = context.diags.newScope(macroExt)) {
				try (CompileContext c2 = new CompileContext(context)) {
					vc.writeCacheCode(c2);
					installMacroArgs(c2);
					innards.readInplaceCompile(out, c2);
				}
			}
		}

		@Override
		protected String getInlineCAOSInner(int index, boolean write, CompileContextNW context) {
			if (vc.copies.length != 0)
				return null;
			CompileContextNW c2 = new CompileContextNW(context);
			installMacroArgs(c2);
			return innards.getInlineCAOS(index, write, c2);
		}

		@Override
		protected RALSpecialInline getSpecialInlineInner(int index, CompileContextNW context) {
			if (vc.copies.length != 0)
				return RALSpecialInline.None;
			CompileContextNW c2 = new CompileContextNW(context);
			installMacroArgs(c2);
			return innards.getSpecialInline(index, c2);
		}

		@Override
		public String toString() {
			return "macro arg copier of " + macroName;
		}
	}
}
