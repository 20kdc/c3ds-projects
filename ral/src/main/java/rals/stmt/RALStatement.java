/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.stmt;

import rals.code.ScopeContext;
import rals.lex.SrcPos;

/**
 * Represents a statement inside a RAL script.
 */
public abstract class RALStatement {
	public final SrcPos lineNumber;
	public RALStatement(SrcPos ln) {
		lineNumber = ln;
	}

	public final void compile(StringBuilder writer, ScopeContext scope) {
		writer.append(" * @ ");
		writer.append(lineNumber);
		writer.append("\n");
		try {
			compileInner(writer, scope);
		} catch (Exception ex) {
			throw new RuntimeException("At " + lineNumber, ex);
		}
	}

	/**
	 * Compiles the statement.
	 */
	protected abstract void compileInner(StringBuilder writer, ScopeContext scope);
}
