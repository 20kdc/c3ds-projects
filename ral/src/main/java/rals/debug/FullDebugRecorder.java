/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.debug;

import rals.code.CodeWriter;
import rals.code.CompileContext;

/**
 * Lots of stuff here...
 */
public class FullDebugRecorder extends CommentingDebugRecorder {
	public FullDebugRecorder() {
		super(true);
	}

	@Override
	public boolean shouldGenerateSites() {
		return true;
	}

	@Override
	public void saveSiteAndCreateMarker(CodeWriter caller, DebugSite ds) {
		caller.writeCode("sets va99 \"" + ds.encode() + "\"");
	}

	@Override
	public void initializeRootCC(CompileContext compileContext) {
		compileContext.allocVA(99);
	}
}
