/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.debug;

import cdsp.common.data.bytestring.ByteString;
import rals.caos.CAOSUtils;
import rals.cctx.*;
import rals.code.CodeGenFeatureLevel;

/**
 * Lots of stuff here...
 */
public class FullDebugRecorder extends CommentingDebugRecorder {
	public final CodeGenFeatureLevel codeGenFeatureLevel;

	public FullDebugRecorder(CodeGenFeatureLevel cgfl) {
		super(true);
		codeGenFeatureLevel = cgfl;
	}

	@Override
	public boolean shouldGenerateSites() {
		return true;
	}

	@Override
	public void saveSiteAndCreateMarker(CodeWriter caller, DebugSite ds) {
		// [CAOS]
		int depth = ds.depth;
		while (depth > 0) {
			try {
				ByteString wanted = new ByteString(ds.encode(depth), CAOSUtils.CAOS_CHARSET);
				ByteString.Builder b = new ByteString.Builder(wanted.length() + 2);
				b.writeASCII("sets va99 ");
				CAOSUtils.stringIntoCAOSConstant(b, wanted, codeGenFeatureLevel);
				caller.writeCode(b);
				return;
			} catch (CAOSUtils.ConstantTooLargeForVMException vme) {
				// oops
			}
			depth--;
		}
		// uhoh
		caller.writeComment("Unable to represent frame in acceptable constant string bounds for this VM.");
		caller.writeCode("sets va99 \"\"");
	}

	@Override
	public void initializeRootCC(CompileContext compileContext) {
		compileContext.allocVA(99);
	}
}
