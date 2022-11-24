/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.hcm;

import java.util.LinkedList;

import rals.code.ScopeContext;
import rals.lex.Token;
import rals.parser.IDocPath;
import rals.parser.IncludeParseContext;
import rals.stmt.RALStatementUR;

/**
 * HCM recorder in cases where HCM recording is wanted.
 */
public class ActualHCMRecorder implements IHCMRecorder {
	public final IDocPath targetDocPath;
	public final LinkedList<Token> tokensInTargetFile = new LinkedList<>();

	public ActualHCMRecorder(IDocPath docPath) {
		targetDocPath = docPath;
	}

	@Override
	public void readToken(Token tkn) {
		if (tkn.lineNumber.file.docPath == targetDocPath)
			tokensInTargetFile.add(tkn);
	}

	@Override
	public void idReference(Token tkn) {
		if (tkn.lineNumber.file.docPath == targetDocPath) {
			// bleh
		}
	}

	@Override
	public void statementResolvePre(RALStatementUR rs, ScopeContext scope) {
	}

	@Override
	public void statementResolvePost(RALStatementUR rs, ScopeContext scope) {
	}

	public HCMStorage compile(IncludeParseContext info) {
		HCMStorage hs = new HCMStorage();
		return hs;
	}
}
