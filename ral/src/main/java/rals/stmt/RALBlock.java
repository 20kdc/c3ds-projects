/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.stmt;

import java.util.LinkedList;

import rals.code.ScopeContext;
import rals.lex.SrcPos;

/**
 * Block "statement"
 */
public class RALBlock extends RALStatement {
	public LinkedList<RALStatement> content = new LinkedList<>();
	public RALBlock(SrcPos lineNumber) {
		super(lineNumber);
	}
	@Override
	protected void compileInner(StringBuilder writer, ScopeContext scope) {
		try (ScopeContext innerScope = new ScopeContext(scope)) {
			for (RALStatement rl : content)
				rl.compile(writer, innerScope);
		}
	}
}
