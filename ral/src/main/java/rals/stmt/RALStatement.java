/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.stmt;

import rals.code.*;
import rals.lex.*;

/**
 * Represents a statement inside a RAL script.
 */
public abstract class RALStatement {
	public final SrcPos lineNumber;
	public RALStatement(SrcPos ln) {
		lineNumber = ln;
	}

	public final void compile(CodeWriter writer, CompileContext context) {
		writer.queuedCommentForNextLine = "@ " + lineNumber + " " + this;
		try {
			compileInner(writer, context);
		} catch (Exception ex) {
			throw new RuntimeException("At " + lineNumber, ex);
		}
	}

	/**
	 * Compiles the statement.
	 */
	protected abstract void compileInner(CodeWriter writer, CompileContext context);
}
