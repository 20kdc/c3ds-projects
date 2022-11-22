/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.parser;

import java.util.HashSet;
import java.util.LinkedList;

import rals.code.Scripts;
import rals.diag.DiagRecorder;
import rals.types.TypeSystem;

/**
 * Context for includes/searchpath stuff
 */
public class IncludeParseContext {
	public final boolean outputIncludesToErr;
	public final DiagRecorder diags = new DiagRecorder();
	public final LinkedList<IDocPath> searchPaths = new LinkedList<>();
	public final HashSet<IDocPath> included = new HashSet<>();
	public final TypeSystem typeSystem = new TypeSystem();
	public final Scripts module = new Scripts();
	public IncludeParseContext(boolean err) {
		outputIncludesToErr = err;
	}
}
