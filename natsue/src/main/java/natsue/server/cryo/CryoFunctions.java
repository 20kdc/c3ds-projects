/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package natsue.server.cryo;

import java.util.LinkedList;

import cdsp.common.data.pray.PRAYBlock;
import cdsp.common.data.pray.PRAYTags;
import natsue.data.babel.PacketReader;
import natsue.data.babel.UINUtils;
import natsue.names.CreatureDataVerifier;
import natsue.server.database.UnixTime;
import natsue.server.userdata.INatsueUserData;

/**
 * Functions relating to the import and export of creatures from cryo
 * Note that this DOES NOT acknowledge the existence of EXPC. DS engine, remember?
 */
public class CryoFunctions {
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
	 * Finds a creature root block.
	 * This answers the question "did the user intend to send a creature".
	 */
	public static PRAYBlock findCreatureRootBlock(Iterable<PRAYBlock> inp) {
		for (PRAYBlock pb : inp) {
			String name = pb.getName();
			String type = pb.getType();
			// ignore unknown/old creature export chunks
			if (!(type.equals("DSEX") || type.equals("warp")))
				continue;
			if (name.endsWith("." + type)) {
				String head = monikerFromRootBlock(pb);
				if (CreatureDataVerifier.verifyMoniker(head)) {
					// this is of the form (moniker).TYPE -- this is the creature root
					return pb;
				}
			}
		}
		return null;
	}
	/**
	 * Gets the moniker given a creature root block name.
	 */
	public static String monikerFromRootBlock(PRAYBlock root) {
		String name = root.getName();
		int splitIndex = name.lastIndexOf('.');
		return name.substring(0, splitIndex);
	}
	/**
	 * Sanity checks a creature to see if it's well-formed.
	 * This includes checking for any PRAY blocks that aren't supposed to be there.
	 * If this returns non-null, that is the error.
	 */
	public static String checkWellFormedCreature(Iterable<PRAYBlock> inp) {
		PRAYBlock creatureRoot = findCreatureRootBlock(inp);
		if (creatureRoot == null)
			return "no creature root";
		// check creature root is sane
		try {
			PRAYTags pt = new PRAYTags(PacketReader.CHARSET);
			pt.read(creatureRoot.data);
			int reG = pt.intMap.get("Genus");
			if (reG < 1 || reG > 4)
				return "Invalid creature genus";
		} catch (Exception ex) {
			return "Couldn't read root block tags";
		}
		// continue...
		String cType = creatureRoot.getType();
		boolean hasGLST = false;
		boolean hasCREA = false;
		boolean hasGENE0 = false;
		String moniker = monikerFromRootBlock(creatureRoot);
		String glistName = moniker + "." + cType + ".glist.creature";
		String creaName = moniker + "." + cType + ".creature";
		String geneticSuffix = "." + cType + ".genetics";
		String gene0Name = moniker + geneticSuffix;
		for (PRAYBlock pb : inp) {
			String name = pb.getName();
			String type = pb.getType();
			if (type.equals("GLST")) {
				if (name.equals(glistName)) {
					hasGLST = true;
				} else {
					return "secondary GLST";
				}
			} else if (type.equals("CREA")) {
				if (name.equals(creaName)) {
					hasCREA = true;
				} else {
					return "secondary CREA";
				}
			} else if (type.equals("GENE")) {
				// pregnancies can cause additional GENE chunks
				// therefore we give leniency to added GENE chunks
				if (name.endsWith(geneticSuffix)) {
					if (name.equals(gene0Name))
						hasGENE0 = true;
				} else {
					return "GENE chunk that isn't in the right namespace";
				}
			} else if (type.equals("PHOT")) {
				// right, so, this bit is a bit stupid
				// basically, the receiver won't take it well if we strip these, ever.
				// what we can do is verify the name is what it should be
				if (name.endsWith("." + cType + ".photo")) {
					if (!CreatureDataVerifier.verifyMonikerBase(name.substring(0, name.indexOf('.')), CreatureDataVerifier.MONIKER_CREATURE_COMPONENTS + 1))
						return "PHOT chunk with dodgy name";
				} else {
					return "PHOT chunk that isn't in the right namespace";
				}
			} else if (pb != creatureRoot) {
				return "there's a chunk that isn't supposed to be here: " + type;
			}
		}
		if (!hasGLST)
			return "no GLST";
		if (!hasCREA)
			return "no CREA";
		if (!hasGENE0)
			return "no GENE0";
		return null;
	}
	/**
	 * Conversion of a creature to the given format.
	 * Make sure isWellFormedCreature was used at some point.
	 * Returns false on failure.
	 */
	public static boolean creatureConvertInPlace(Iterable<PRAYBlock> inp, String nType) {
		PRAYBlock creatureRoot = findCreatureRootBlock(inp);
		if (creatureRoot == null)
			return false;
		String oType = creatureRoot.getType();
		creatureRoot.setType(nType);
		creatureRoot.setName(monikerFromRootBlock(creatureRoot) + "." + nType);
		// note that "." cannot occur "naturally" & types are known
		for (PRAYBlock blk : inp)
			blk.setName(blk.getName().replace("." + oType + ".", "." + nType + "."));
		return true;
	}
	/**
	 * Stash data in the root. In case of abuse of Cryo this data can be retrieved.
	 */
	public static void cryoUpdateRootStorage(PRAYBlock inp, long fromUIN) {
		PRAYTags pt = new PRAYTags(PacketReader.CHARSET);
		pt.read(inp.data);
		pt.strMap.put("Pray Extra natsueCryoSubmitter", UINUtils.toString(fromUIN));
		pt.intMap.put("Pray Extra natsueCryoSubmitTime", (int) UnixTime.get());
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
		inp.data = pt.toByteArray();
	}
	/**
	 * Returns a reason for receipt incompatibility (if any).
	 */
	public static String receiptCompatibilityCheck(LinkedList<PRAYBlock> messageBlocks, INatsueUserData destUser) {
		PRAYBlock block = CryoFunctions.findCreatureRootBlock(messageBlocks);
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
