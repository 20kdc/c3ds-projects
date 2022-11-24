/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.hcm;

import rals.code.ScopeContext;
import rals.diag.SrcPosUntranslated;
import rals.parser.IDocPath;
import rals.stmt.RALStatementUR;

/**
 * HCM recorder in cases where HCM recording is wanted.
 */
public class ActualHCMRecorder implements IHCMRecorder {
	public final IDocPath targetDocPath;

	public ActualHCMRecorder(IDocPath docPath) {
		targetDocPath = docPath;
	}

	@Override
	public void statementResolvePre(RALStatementUR rs, ScopeContext scope) {
	}

	@Override
	public void statementResolvePost(RALStatementUR rs, ScopeContext scope) {
	}

	@Override
	public HoverData getHoverData(SrcPosUntranslated tkn) {
		return new HoverData("This is a rubber ducky");
	}
}
