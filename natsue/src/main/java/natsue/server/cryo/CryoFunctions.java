/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package natsue.server.cryo;

import java.util.LinkedList;

import cdsp.common.data.pray.ExportedCreatures;
import cdsp.common.data.pray.PRAYBlock;
import cdsp.common.data.pray.PRAYTags;
import cdsp.common.util.UnixTime;
import natsue.data.babel.PacketReader;
import natsue.data.babel.UINUtils;
import natsue.server.userdata.INatsueUserData;

/**
 * Functions relating to the import and export of creatures from cryo
 * Note that this DOES NOT acknowledge the existence of EXPC. DS engine, remember?
 */
public class CryoFunctions {
	/**
	 * Returns true if chunks exist that imply a creature must exist for transmission on the Warp.
	 */
	public static boolean expectedToContainACreature(Iterable<PRAYBlock> inp) {
		for (PRAYBlock pb : inp) {
			String t = pb.getType();
			if (t.equals("warp") || t.equals("DSEX"))
				return true;
			if (t.equals("GENE") || t.equals("PHOT") || t.equals("CREA") || t.equals("GLST"))
				return true;
		}
		return false;
	}

	/**
	 * Stash data in the root. In case of abuse of Cryo this data can be retrieved.
	 */
	public static void cryoUpdateRootStorage(PRAYBlock inp, long fromUIN) {
		PRAYTags pt = new PRAYTags(PacketReader.CHARSET);
		pt.read(inp.data);
		pt.strMap.put("Pray Extra natsueCryoSubmitter", UINUtils.toString(fromUIN));
		pt.intMap.put("Pray Extra natsueCryoSubmitTime", (int) UnixTime.get());
		pt.intMap.put("Pray Extra natsueCryoSubmitTimeH", (int) (UnixTime.get() >> 32));
		inp.data = pt.toByteArray();
	}
	/**
	 * Remove stashed data.
	 */
	public static void cryoUpdateRootRetrieval(PRAYBlock inp, long fromUIN) {
		PRAYTags pt = new PRAYTags(PacketReader.CHARSET);
		pt.read(inp.data);
		pt.strMap.remove("Pray Extra natsueCryoSubmitter");
		pt.intMap.remove("Pray Extra natsueCryoSubmitTime");
		pt.intMap.remove("Pray Extra natsueCryoSubmitTimeH");
		inp.data = pt.toByteArray();
	}
	/**
	 * Returns a reason for receipt incompatibility (if any).
	 */
	public static String receiptCompatibilityCheck(LinkedList<PRAYBlock> messageBlocks, INatsueUserData destUser) {
		PRAYBlock block = ExportedCreatures.findCreatureRootBlock(messageBlocks);
		// Handle tags and so forth
		// NB norn detector
		PRAYTags pt = new PRAYTags(PacketReader.CHARSET);
		pt.read(block.data);
		int reC = pt.intMap.get("Gender");
		int reG = pt.intMap.get("Genus");
		boolean isNB = reC != 1 && reC != 2;
		boolean isGeat = reG == 4;
		if (isGeat && !destUser.isReceivingGeats()) {
			// Wasteland glitch prevention (CACL 4 4 0 19!!!!)
			return "Geat that target couldn't receive";
		} else if (isNB && !destUser.isReceivingNBNorns()) {
			// NB norns crash people who aren't prepared to receive them.
			return "NB norn that target couldn't receive";
		}
		return null;
	}
}
