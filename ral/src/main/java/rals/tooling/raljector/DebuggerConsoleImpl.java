/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.tooling.raljector;

import java.util.function.Function;

import rals.tooling.Injector;
import rals.tooling.raljector.DebuggerDialog.FilterLibMode;

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
		if (var1.startsWith("caos ")) {
			try {
				return Injector.cpxRequest("execute\n" + var1.substring(5)) + "\n";
			} catch (Exception ex) {
				ex.printStackTrace();
				return "Error in injection: " + ex.getMessage() + "\n";
			}
		}
		String[] args = var1.split(" ");
		if (args.length == 0)
			return "";
		if (args[0].equalsIgnoreCase("help")) {
			return
					"help: Help on debugger commands\n" +
					"bp+ FAMILY GENUS SPECIES SCRIPT: Sets a breakpoint at the given script.\n" +
					"bp- FAMILY GENUS SPECIES SCRIPT: Unsets a breakpoint at the given script.\n" +
					"sp [STEPS]: Advances exactly one step, or up to a given amount of steps. These are not lines.\n" +
					"s: Advances one step, skipping over metadata.\n" +
					"so: Steps over the current frame (contained frames are skipped)\n" +
					"ns: Skips to the next RAL debug metadata (should be roughly the next statement).\n" +
					"c: Unpauses the game.\n" +
					"filterLib caos/stdlib/none: Controls scope filtering.\n" +
					"caos (CAOS...): Sends CAOS directly to the game.\n" +
					"vv VALUE: views agent with unid VALUE"
			;
		} else if (args[0].equalsIgnoreCase("bp+")) {
			if (args.length != 5)
				return "bp+ FAMILY GENUS SPECIES SCRIPT\n";
			return bp(args, true);
		} else if (args[0].equalsIgnoreCase("bp-")) {
			if (args.length != 5)
				return "bp- FAMILY GENUS SPECIES SCRIPT\n";
			return bp(args, false);
		} else if (args[0].equalsIgnoreCase("sp")) {
			if (args.length > 2)
				return "sp [STEPS]\n";
			int sc = 1;
			if (args.length == 2)
				sc = Integer.parseInt(args[1]);
			if (sc >= 1) {
				state.tackPlay(DebugStepDecider.steps(sc, DebugStepDecider.immutableToMutable(DebugStepDecider.NO_SKIP)).apply(state.lastDebugFrame));
			}
			return "Running...\n";
		} else if (args[0].equalsIgnoreCase("s")) {
			if (dbg.processedFrame == null)
				return "no current frame\n";
			state.tackPlay(DebugStepDecider.SKIP_METADATA);
			return "Running...\n";
		} else if (args[0].equalsIgnoreCase("so")) {
			if (dbg.processedFrame == null)
				return "no current frame\n";
			state.tackPlay(DebugStepDecider.stepOver(dbg.processedFrame));
			return "Running...\n";
		} else if (args[0].equalsIgnoreCase("ns")) {
			state.tackPlay(DebugStepDecider.metadataAndThenOne());
			return "Running...\n";
		} else if (args[0].equalsIgnoreCase("c")) {
			state.dbgPlay();
			return "Running...\n";
		} else if (args[0].equalsIgnoreCase("filterLib")) {
			if (args.length != 2)
				return "filterLib caos/stdlib/none";
			dbg.filterLib = FilterLibMode.valueOf(args[1]);
			return "Done.\n";
		} else if (args[0].equalsIgnoreCase("vv")) {
			if (args.length != 2)
				return "vv AGNT";
			dbg.openValueInspector("vv", () -> args[1]);
		}
		return "Unknown command: " + args[0] + " (try help?)\n";
	}

	private String bp(String[] args, boolean b) {
		int f = Integer.parseInt(args[1]);
		int g = Integer.parseInt(args[2]);
		int s = Integer.parseInt(args[3]);
		int e = Integer.parseInt(args[4]);
		if (state.doNotInject())
			return "Game potentially mid-processing, will not inject\n";
		String magic = "dbg: tack ownr ";
		String fgse = "" + f + " " + g + " " + s + " " + e;
		try {
			String code = Injector.cpxRequest("execute\nouts sorc " + fgse + "\n");
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
			return Injector.cpxRequest("scrp " + fgse + "\n" + code) + "\n";
		} catch (Exception ex) {
			ex.printStackTrace();
			return "Error in injection: " + ex.getMessage() + "\n";
		}
	}
}
