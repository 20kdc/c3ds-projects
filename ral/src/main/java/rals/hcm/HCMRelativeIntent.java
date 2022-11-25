/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.hcm;

import java.util.Map;

import rals.diag.SrcPosUntranslated;
import rals.expr.RALExprSlice;
import rals.expr.RALExprUR;
import rals.hcm.HCMStorage.HoverData;
import rals.lex.Token;

/**
 * An intent relative to some expressions.
 */
public abstract class HCMRelativeIntent extends HCMIntent {
	public final int expressionCount;
	public final HCMIntent fallback;
	public HCMRelativeIntent(int ec, HCMIntent fb) {
		expressionCount = ec;
		fallback = fb;
	}

	@Override
	public final Map<String, HoverData> retrieve(Token sp, SrcPosUntranslated spu, HCMStorage storage) {
		Tracking ex = storage.relativeIntentExprs.get(new Anchor(this, sp));
		if (ex == null || !ex.isFinished())
			return fallback.retrieve(sp, spu, storage);
		return retrieveParameterized(sp, spu, storage, ex.resolvedExpressions);
	}

	public abstract Map<String, HoverData> retrieveParameterized(Token sp, SrcPosUntranslated spu, HCMStorage storage, RALExprSlice[] exprs);

	/**
	 * Tracking object for unresolved HCM relative intents.
	 */
	public static class Tracking {
		public final RALExprUR[] baseExpressions;
		public final RALExprSlice[] resolvedExpressions;
		public Tracking(RALExprUR[] be) {
			baseExpressions = be;
			resolvedExpressions = new RALExprSlice[be.length];
		}
		public void contribute(RALExprUR a, RALExprSlice b) {
			for (int i = 0; i < baseExpressions.length; i++)
				if (a == baseExpressions[i])
					resolvedExpressions[i] = b;
		}
		public boolean isFinished() {
			for (int i = 0; i < baseExpressions.length; i++)
				if (resolvedExpressions[i] == null)
					return false;
			return true;
		}
	}

	public static class Anchor {
		public final HCMRelativeIntent intent;
		/**
		 * This token matches up with intentsOnNextToken!
		 */
		public final Token token;
		public Anchor(HCMRelativeIntent ref, Token tkn) {
			intent = ref;
			token = tkn;
		}
		@Override
		public int hashCode() {
			return intent.hashCode() ^ token.hashCode();
		}
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Anchor) {
				Anchor a = (Anchor) obj;
				return a.token == token && a.intent == intent;
			}
			return false;
		}
	}
}
