/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.debug;

import rals.cctx.*;
import rals.stmt.RALStatement;

/**
 * Does no commenting
 */
public class DummyDebugRecorder implements IDebugRecorder {
	public DummyDebugRecorder() {
	}

	@Override
	public String createQueuedComment(RALStatement rs) {
		return null;
	}

	@Override
	public boolean shouldGenerateSites() {
		return false;
	}

	@Override
	public void saveSiteAndCreateMarker(CodeWriter caller, DebugSite ds) {
	}

	@Override
	public void initializeRootCC(CompileContext compileContext) {
	}
}
