/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.parser;

import rals.code.ScriptsUR;
import rals.diag.DiagRecorder;
import rals.lex.Lexer;
import rals.types.TypeSystem;

/**
 * Context for parsing inside of a file.
 */
public class InsideFileContext {
	public final DiagRecorder diags;
	public final Lexer lexer;
	public final TypeSystem typeSystem;
	public final ScriptsUR module;
	public InsideFileContext(IncludeParseContext ipc, Lexer lx) {
		typeSystem = ipc.typeSystem;
		module = ipc.module;
		lexer = lx;
		diags = ipc.diags;
	}
}
