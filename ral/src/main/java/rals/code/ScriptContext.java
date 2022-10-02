/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.code;

import rals.types.RALType;
import rals.types.TypeSystem;

/**
 * General context for compilation and such.
 */
public class ScriptContext {
	public final RALType ownrType;
	public final RALType fromType;
	public final RALType p1Type;
	public final RALType p2Type;
	public final TypeSystem typeSystem;
	public final Scripts module;

	public ScriptContext(TypeSystem ts, Scripts m, RALType ot, RALType ft, RALType p1, RALType p2) {
		typeSystem = ts;
		module = m;
		ownrType = ot;
		fromType = ft;
		p1Type = p1;
		p2Type = p2;
	}
}
