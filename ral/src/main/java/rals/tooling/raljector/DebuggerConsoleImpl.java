/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.tooling.raljector;

import java.util.Collections;
import java.util.LinkedList;
import java.util.function.Function;

import cdsp.common.cpx.Injector;
import rals.caos.CAOSUtils;
import rals.tooling.raljector.DebuggerDialog.FilterLibMode;
import rals.types.Classifier;

/**
 * Implements the debugger console.
 */
public class DebuggerConsoleImpl implements Function<String, String> {
	public final GameStateTracker state;
	public final DebuggerDialog dbg;

	public DebuggerConsoleImpl(GameStateTracker st, DebuggerDialog d) {
		state = st;
		dbg = d;
	}

	@Override
	public String apply(String var1) {
		if (var1.startsWith("/")) {
			try {
				return Injector.cpxRequest("execute\n" + var1.substring(1), CAOSUtils.CAOS_CHARSET) + "\n";
			} catch (Exception ex) {
				ex.printStackTrace();
				return "Error in injection: " + ex.getMessage() + "\n";
			}
		}
		StringArrayStream cmd = new StringArrayStream(var1.split(" "));
		if (!cmd.hasMore())
			return "";
		String cmdName = cmd.get();
		if (cmdName.equalsIgnoreCase("help")) {
			return
					"/(CAOS...): Sends CAOS directly to the game.\n" +
					"bp+ (F G S/NAME) SCRIPT: Sets a breakpoint at the given script.\n" +
					"bp- (F G S/NAME) SCRIPT: Unsets a breakpoint at the given script.\n" +
					"c: Unpauses the game.\n" +
					"classes: Lists all classes in loaded taxonomy\n" +
					"cls: Clears debugger console display.\n" +
					"filterLib [caos/stdlib/none]: Controls scope filtering (or shows current if not provided).\n" +
					"help: Help on debugger commands\n" +
					"sp [STEPS]: Advances exactly one step, or up to a given amount of steps. These are not lines.\n" +
					"s: Advances one step, skipping over metadata.\n" +
					"so: Steps over the current frame (contained frames are skipped)\n" +
					"ns: Skips to the next RAL debug metadata (should be roughly the next statement).\n" +
					"vv VALUE: views agent with unid VALUE\n" +
					"vr (F G S/NAME): views rtar'd agent with this classifier\n"
			;
		} else if (cmdName.equalsIgnoreCase("bp+")) {
			cmd.help = "bp+ (F G S/NAME) SCRIPT";
			return bp(cmd, true);
		} else if (cmdName.equalsIgnoreCase("bp-")) {
			cmd.help = "bp- (F G S/NAME) SCRIPT";
			return bp(cmd, false);
		} else if (cmdName.equalsIgnoreCase("classes")) {
			cmd.help = "classes";
			cmd.expectNoMore();
			if (state.debugTaxonomy == null)
				return "No taxonomy loaded\n";
			StringBuilder res = new StringBuilder();
			LinkedList<String> lls = new LinkedList<>();
			for (String s : state.debugTaxonomy.allNamesIterable())
				lls.add(s);
			Collections.sort(lls);
			for (String s : lls) {
				res.append(s);
				res.append(": ");
				res.append(state.debugTaxonomy.classifierByName(s));
				res.append("\n");
			}
			res.append(lls.size());
			res.append(" names\n");
			return res.toString();
		} else if (cmdName.equalsIgnoreCase("sp")) {
			cmd.help = "sp [STEPS]";
			int sc = 1;
			if (cmd.hasMore())
				sc = Integer.parseInt(cmd.get());
			cmd.expectNoMore();
			if (sc >= 1) {
				DebugStepDecider.Mut dsd = DebugStepDecider.steps(sc, DebugStepDecider.immToMut(DebugStepDecider.NO_SKIP));
				state.tackPlay(dsd.apply(state.lastDebugFrame));
			}
			return "Running...\n";
		} else if (cmdName.equalsIgnoreCase("s")) {
			cmd.help = "s";
			cmd.expectNoMore();
			if (dbg.processedFrame == null)
				return "no current frame\n";
			state.tackPlay(DebugStepDecider.SKIP_METADATA);
			return "Running...\n";
		} else if (cmdName.equalsIgnoreCase("so")) {
			cmd.help = "so";
			cmd.expectNoMore();
			if (dbg.processedFrame == null)
				return "no current frame\n";
			state.tackPlay(DebugStepDecider.stepOver(dbg.processedFrame));
			return "Running...\n";
		} else if (cmdName.equalsIgnoreCase("ns")) {
			cmd.help = "ns";
			cmd.expectNoMore();
			state.tackPlay(DebugStepDecider.metadataAndThenOne());
			return "Running...\n";
		} else if (cmdName.equalsIgnoreCase("c")) {
			cmd.help = "c";
			cmd.expectNoMore();
			state.dbgPlay();
			return "Running...\n";
		} else if (cmdName.equalsIgnoreCase("filterLib")) {
			cmd.help = "filterLib [caos/stdlib/none]";
			if (cmd.hasMore()) {
				String mode = cmd.get();
				cmd.expectNoMore();
				dbg.filterLib = FilterLibMode.valueOf(mode);
				return "Done.\n";
			} else {
				return dbg.filterLib + "\n";
			}
		} else if (cmdName.equalsIgnoreCase("vv")) {
			cmd.help = "vv AGNT";
			String agnt = cmd.get();
			cmd.expectNoMore();
			doVV(agnt);
			return "Done.\n";
		} else if (cmdName.equalsIgnoreCase("vr")) {
			cmd.help = "vr (NAME/F G S)";
			Classifier cls = classifierArg(cmd, 3);
			cmd.expectNoMore();
			try {
				doVV(Injector.cpxRequest("execute\nrtar " + cls.toCAOSString() + "\noutv unid\n", CAOSUtils.CAOS_CHARSET).trim());
			} catch (Exception ex) {
				return "Error: " + ex.getMessage() + "\n";
			}
			return "Done.\n";
		}
		return "Unknown command: " + cmdName + " (try help?)\n";
	}

	private void doVV(final String arg) {
		new ValueMonitorDialog(state, "vv:" + arg, () -> arg);
	}

	private Classifier classifierArg(StringArrayStream args, int decision) {
		if (args.remaining() >= decision) {
			int f = Integer.parseInt(args.get());
			int g = Integer.parseInt(args.get());
			int s = Integer.parseInt(args.get());
			return new Classifier(f, g, s);
		} else {
			String name = args.get();
			if (state.debugTaxonomy == null)
				throw new RuntimeException("Hasn't compiled, so no debug taxonomy - use View CAOS or an inject button");
			Classifier cls = state.debugTaxonomy.classifierByName(name);
			if (cls == null)
				throw new RuntimeException("Unable to find classifier for " + name);
			return cls;
		}
	}

	private String bp(StringArrayStream args, boolean b) {
		Classifier cls = classifierArg(args, 4);
		int e = Integer.parseInt(args.get());
		args.expectNoMore();
		if (state.doNotInject())
			return "Game potentially mid-processing, will not inject\n";
		String magic = "dbg: tack ownr ";
		String fgse = cls.toCAOSString() + " " + e;
		try {
			String code = Injector.cpxRequest("execute\nouts sorc " + fgse + "\n", CAOSUtils.CAOS_CHARSET);
			if (!b) {
				// want to get rid of it
				if (code.startsWith(magic)) {
					code = code.substring(magic.length());
				} else {
					return "No alteration necessary";
				}
			} else {
				// want to add it
				if (code.startsWith(magic)) {
					return "No alteration necessary";
				} else {
					code = magic + code;
				}
			}
			return Injector.cpxRequest("scrp " + fgse + "\n" + code, CAOSUtils.CAOS_CHARSET) + "\n";
		} catch (Exception ex) {
			ex.printStackTrace();
			return "Error in injection: " + ex.getMessage() + "\n";
		}
	}
}
