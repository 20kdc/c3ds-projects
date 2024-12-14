/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.tooling.raljector;

import java.util.function.Function;

import cdsp.common.cpx.Injector;

public class GameStateTracker {
	/**
	 * Currently tacked agent
	 */
	public int tackUNID = -1;

	/**
	 * Current game state
	 */
	public State gameState = State.Offline;

	/**
	 * Game state change tracker
	 */
	private State lastGameState = State.Offline;

	private boolean newExceptionsNeedToBeShown = true;

	public final Signal<State> stateChange = new Signal<>();
	public RawDebugFrame lastDebugFrame;
	public final Signal<RawDebugFrame> debugFrame = new Signal<>();
	public final Signal<String> displayMessageToUser = new Signal<>();
	public final Signal<Object> additionalRefreshTasks = new Signal<>();
	private Function<RawDebugFrame, Boolean> stepDecider = DebugStepDecider.SKIP_METADATA;

	/**
	 * Taxonomy imported from last compile.
	 */
	public DebugTaxonomyData debugTaxonomy;

	public void update() {
		try {
			String res = Injector.cpxRequest(
				"execute\n" +
				// 0 [6]
				"doif tack <> null\n" +
				" targ tack\n" +
				" outv unid\n" +
				" outs \"\\n\"\n" +
				" outv codf\n" +
				" outs \"\\n\"\n" +
				" outv codg\n" +
				" outs \"\\n\"\n" +
				" outv cods\n" +
				" outs \"\\n\"\n" +
				" outv code\n" +
				" outs \"\\n\"\n" +
				" outv codp\n" +
				" outs \"\\n\"\n" +
				"else\n" +
				" outs \"-1\\n\"\n" +
				" outs \"-1\\n\"\n" +
				" outs \"-1\\n\"\n" +
				" outs \"-1\\n\"\n" +
				" outs \"-1\\n\"\n" +
				" outs \"-1\\n\"\n" +
				"endi\n" +
				// 6 [1]
				"outv paws\n" +
				"outs \"\\n\"\n" +
				"doif paws <> 0\n" +
				" dbg: flsh\n" +
				"endi\n" +
				// 7 [109]
				"doif tack <> null\n" +
				" setv va00 -9\n" +
				" targ tack\n" +
				" reps 109\n" +
				"  outx dbg# va00\n" +
				"  outs \"\\n\"\n" +
				"  addv va00 1\n" +
				" repe\n" +
				"else\n" +
				" reps 109\n" +
				"  outs \"\\n\"\n" +
				" repe\n" +
				"endi\n" +
				""
			);
			String[] lines = res.split("\n");
			tackUNID = Integer.parseInt(lines[0]);
			int codf = Integer.parseInt(lines[1]);
			int codg = Integer.parseInt(lines[2]);
			int cods = Integer.parseInt(lines[3]);
			int code = Integer.parseInt(lines[4]);
			int codp = Integer.parseInt(lines[5]);
			int isPaws = Integer.parseInt(lines[6]);

			if (tackUNID != -1) {
				if (code != -1) {
					RawDebugFrame rdf = null;
					// Skips line number commands that RAL injected in the first place
					boolean didCheese = false;
					// Only do this part the first time
					if (lastGameState != State.Tacked) {
						String src = Injector.cpxRequest( 
							"execute\nouts sorc " + codf + " " + codg + " " + cods + " " + code
						);
						rdf = new RawDebugFrame(lines, 7, codf, codg, cods, code, codp, src);
						didCheese = stepDecider.apply(rdf);
						if (!didCheese) {
							// reset to default when a sequence completes
							stepDecider = DebugStepDecider.SKIP_METADATA;
						} else {
							// never happened, honest
							rdf = null;
							Injector.cpxRequest("execute\ndbg: tack tack");
						}
					}
					gameState = didCheese ? State.RunningTack : State.Tacked;
					if (rdf != null) {
						lastDebugFrame = rdf;
						debugFrame.fire(rdf);
					}
				} else {
					gameState = State.RunningTack;
				}
			} else {
				if (isPaws == 0) {
					if (gameState != State.RunningTack)
						gameState = State.Running;
				} else {
					gameState = State.Pawsed;
				}
			}
			// System.out.println(tackUNID + " " + isPaws);
			newExceptionsNeedToBeShown = true;
		} catch (Exception ex) {
			if (newExceptionsNeedToBeShown) {
				ex.printStackTrace();
				newExceptionsNeedToBeShown = false;
			}
			tackUNID = -1;
			gameState = State.Offline;
		}
		updateGameState();
		additionalRefreshTasks.fire(null);
	}

	public boolean doNotInject() {
		return gameState == State.Tacked || gameState == State.RunningTack;
	}

	private void updateGameState() {
		// evaluate state transitions
		if (lastGameState != gameState) {
			lastGameState = gameState;
			stateChange.fire(gameState);
		}
	}

	public void tackPlay(Function<RawDebugFrame, Boolean> sd) {
		stepDecider = sd;
		try {
			Injector.cpxRequest("execute\ndbg: tack tack");
			gameState = State.RunningTack;
		} catch (Exception ex) {
			gameState = State.Offline;
		}
		updateGameState();
	}

	public void dbgPlay() {
		stepDecider = DebugStepDecider.SKIP_METADATA;
		try {
			Injector.cpxRequest("execute\ndbg: play");
			gameState = State.Running;
		} catch (Exception ex) {
			gameState = State.Offline;
		}
		updateGameState();
	}

	public enum State {
		Offline,
		Running,
		RunningTack,
		Pawsed,
		Tacked
	}
}
