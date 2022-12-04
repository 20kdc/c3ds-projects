/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.debug;

import rals.code.CodeWriter;
import rals.code.CompileContext;
import rals.stmt.RALStatement;

/**
 * Responsible for recording debugging information.
 */
public interface IDebugRecorder {
	/**
	 * Creates a comment to queue
	 */
	public String createQueuedComment(RALStatement rs);

	/**
	 * If debug sites should be generated
	 */
	public boolean shouldGenerateSites();

	/**
	 * Saves a debug site into storage and creates a marker
	 */
	public void saveSiteAndCreateMarker(CodeWriter caller, DebugSite ds);

	/**
	 * Does anything fancy to the root of a CompileContext
	 */
	public void initializeRootCC(CompileContext compileContext);
}
