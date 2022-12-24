/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.tooling.raljector;

import java.util.function.Function;

/**
 * Stuff for deciding when to do debug steps
 */
public interface DebugStepDecider extends Function<RawDebugFrame, Boolean> {
	/**
	 * Skips metadata.
	 */
	public static final DebugStepDecider SKIP_METADATA = (rdf) -> {
		// determine if we should cheese things
		if (rdf.caosOffset < rdf.caos.length())
			if (rdf.caos.substring(rdf.caosOffset).startsWith("sets va99 \""))
				return true;
		return false;
	};

	/**
	 * Useful for precision.
	 */
	public static final DebugStepDecider NO_SKIP = (rdf) -> {
		return false;
	};

	/**
	 * Converts an immutable step decider to a mutable one
	 */
	public static Mut immToMut(DebugStepDecider imm) {
		return (rdf) -> imm;
	}
	/**
	 * Creates a mutable step decider that runs through another step decider a number of times.
	 */
	public static Mut steps(final int sc, Mut interior) {
		return (first) -> {
			return new DebugStepDecider() {
				// steps remaining. for a single step, this is 1, so becomes 0 and stop
				int sc2 = sc;
				Function<RawDebugFrame, Boolean> currentCycle = interior.apply(first);
				@Override
				public Boolean apply(RawDebugFrame var1) {
					boolean res1 = currentCycle.apply(var1);
					// if the interior stepper returns true, it's busy
					if (res1)
						return true;
					// it returned false, so work out if we actually want to return false
					sc2--;
					if (sc2 > 0) {
						// we don't, so new cycle
						currentCycle = interior.apply(var1);
						return true;
					}
					return false;
				}
			};
		};
	}

	public static DebugStepDecider stepOver(ProcessedDebugFrame processedFrame) {
		final String avoidLI = processedFrame.lineIdentifier;
		return (rdf) -> {
			if (SKIP_METADATA.apply(rdf))
				return true;
			ProcessedDebugFrame[] frames = ProcessedDebugFrame.process(rdf);
			for (ProcessedDebugFrame av : frames)
				if (av.lineIdentifier.equals(avoidLI))
					return true;
			return false;
		};
	}

	public static DebugStepDecider metadataAndThenOne() {
		return new DebugStepDecider() {
			boolean hasReachedAtLeastOneMD = false;
			@Override
			public Boolean apply(RawDebugFrame var1) {
				if (SKIP_METADATA.apply(var1)) {
					// this means we're at the metadata
					// so we need to go just after that
					hasReachedAtLeastOneMD = true;
					return true;
				} else if (hasReachedAtLeastOneMD) {
					// already reached a metadata, so this will be what comes after
					return false;
				} else {
					// haven't reached a metadata yet
					return true;
				}
			}
		};
	}

	/**
	 * Mutable debug step decider. Calling this instances the mutable step decider.
	 */
	public static interface Mut extends Function<RawDebugFrame, DebugStepDecider> {
	}
}
