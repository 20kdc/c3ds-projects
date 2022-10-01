/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.expr;

import rals.code.ScopeContext;
import rals.types.RALType;

/**
 * It's const, but not const.
 * We don't know it until resolution.
 */
public class RALMessageIDGrabber implements RALExprUR {
	public final RALExprUR reference;
	public final String msgName;
	public RALMessageIDGrabber(RALExprUR ref, String name) {
		reference = ref;
		msgName = name;
	}

	@Override
	public RALExpr resolve(ScopeContext scope) {
		RALType base = reference.resolve(new ScopeContext(scope)).assertOutTypeSingle();
		Integer id = base.lookupMSID(msgName, false);
		if (id == null)
			throw new RuntimeException("No message name " + msgName + " in " + base);
		return new RALConstant.Int(scope.script.typeSystem, id);
	}

}
