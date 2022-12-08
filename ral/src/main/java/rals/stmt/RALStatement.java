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
		DebugSite savedSite = context.currentDebugSite;
		if (writer.debug.shouldGenerateSites()) {
			context.currentDebugSite = new DebugSite(savedSite, extent.start.toUntranslated(), context);
			writer.queuedSiteForNextLine = context.currentDebugSite;
		}
		context.diags.pushFrame(extent);
		// Actually compile
		try {
			compileInner(writer, context);
		} catch (Exception ex) {
			context.diags.error("stmt compile: ", ex);
		}
		// Pop diags/debug
		context.diags.popFrame(extent);
		context.currentDebugSite = savedSite;
	}

	/**
	 * Compiles the statement.
	 */
	protected abstract void compileInner(CodeWriter writer, CompileContext context);
}
