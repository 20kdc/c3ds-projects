/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.stmt;

import rals.cctx.*;
import rals.debug.DebugSite;
import rals.diag.SrcRange;

/**
 * Represents a resolved statement inside a RAL script.
 */
public abstract class RALStatement {
	public final SrcRange extent;
	public RALStatement(SrcRange ln) {
		extent = ln;
	}

	public final void compile(CodeWriter writer, CompileContext context) {
		if (writer.queuedCommentForNextLine == null)
			writer.queuedCommentForNextLine = writer.debug.createQueuedComment(this);
		// Push diags/debug
		DebugSite newSite = null;
		if (writer.debug.shouldGenerateSites())
			newSite = new DebugSite(context.currentDebugSite, extent.start.toUntranslated(), context);
		try (CompileContext c2 = context.forkDebugDiagExtent(newSite, extent)) {
			if (newSite != null)
				writer.queuedSiteForNextLine = c2.currentDebugSite;
			// Actually compile
			try {
				compileInner(writer, c2);
			} catch (Exception ex) {
				context.diags.error("stmt compile: ", ex);
			}
		}
	}

	/**
	 * Compiles the statement.
	 */
	protected abstract void compileInner(CodeWriter writer, CompileContext context);
}
