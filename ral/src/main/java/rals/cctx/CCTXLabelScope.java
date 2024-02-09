/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.cctx;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

/**
 * Label scope and container for IBreakHandler.
 */
public class CCTXLabelScope {
	/**
	 * Labels in this label scope
	 * WARNING: Keep consistent with usedLabels OR ELSE.
	 */
	private HashMap<ILabelHandle, String> labels = new HashMap<>();
	/**
	 * Used labels (for unused label elimination)
	 */
	private HashSet<ILabelHandle> usedLabels = new HashSet<>();
	/**
	 * Parent scope, if any
	 */
	public final CCTXLabelScope parent;
	/**
	 * Escape action, should only be null if parent is
	 */
	public final IBreakHandler escapeAction;

	public CCTXLabelScope(CCTXLabelScope p, IBreakHandler ea) {
		parent = p;
		escapeAction = ea;
	}

	public CCTXLabelScope() {
		parent = null;
		escapeAction = null;
	}

	public void addLabel(ILabelHandle lbl, String name) {
		if (labels.containsKey(lbl))
			throw new RuntimeException("Cannot redefine label handle " + lbl + " in the same context (it makes usage tracking dodgy)");
		labels.put(lbl, name);
	}

	public boolean isDefinedHereUsed(ILabelHandle lbl) {
		return usedLabels.contains(lbl);
	}

	/**
	 * Goes to a label, unwinding the stack as necessary.
	 */
	public void compileJump(ILabelHandle break1, CompileContext context) {
		LinkedList<IBreakHandler> handlers = new LinkedList<>();
		CCTXLabelScope here = this;
		while (true) {
			String hitHere = here.labels.get(break1);
			if (hitHere == null) {
				if (here.parent == null)
					throw new RuntimeException("Attempted to compile jump to non-existent " + break1);
				handlers.add(here.escapeAction);
				here = here.parent;
			} else {
				for (IBreakHandler ibh : handlers)
					ibh.compile(context);
				// [CAOS]
				context.writer.writeCode("goto " + hitHere);
				// Note that we add the used label at the point of definition.
				// This is because of handle reuse.
				here.usedLabels.add(break1);
				return;
			}
		}
	}
}
