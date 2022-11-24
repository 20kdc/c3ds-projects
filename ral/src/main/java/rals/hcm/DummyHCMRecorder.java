/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.hcm;

import rals.code.ScopeContext;
import rals.diag.SrcRange;
import rals.lex.Token;

/**
 * HCM recorder in cases where HCM recording is not wanted.
 */
public class DummyHCMRecorder implements IHCMRecorder {
	@Override
	public void readToken(Token tkn) {
	}

	@Override
	public void idReference(Token.ID tkn) {
	}

	@Override
	public void namedTypeReference(Token.ID tkn) {
	}

	@Override
	public void resolvePre(SrcRange rs, ScopeContext scope) {
	}

	@Override
	public void resolvePost(SrcRange rs, ScopeContext scope) {
	}
}
