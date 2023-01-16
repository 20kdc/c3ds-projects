/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package natsue.server.cryo;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;

import natsue.config.Config;
import natsue.data.babel.PacketReader;
import natsue.data.pray.PRAYBlock;
import natsue.log.ILogProvider;
import natsue.log.ILogSource;
import natsue.names.CreatureDataVerifier;
import natsue.server.userdata.INatsueUserData;

/**
 * Interface between Cryo and System which handles sanity checking
 */
public class CryoFrontend implements ILogSource {
	private final ICryogenicStorage storage;
	private final ILogProvider logParent;
	private final Config config;
	private final HashSet<String> lockedFNs = new HashSet<>();
	private Random rnd = new Random();

	public CryoFrontend(Config c, ILogProvider ip) {
		config = c;
		storage = new FileCryogenicStorage(new File(c.cryo.cryoDir.getValue()), ip);
		logParent = ip;
	}
	@Override
	public ILogProvider getLogParent() {
		return logParent;
	}

	/**
	 * Attempt to accept the creature (or fail trying, in which case a string is returned)
	 */
	public String tryAcceptCreature(Iterable<PRAYBlock> pm, INatsueUserData who) {
		if (!config.cryo.cryoSubmitEnabled.getValue())
			return "Cryo submit not enabled";
		if (!config.cryo.cryoSubmitPublic.getValue())
			if (!who.isAdmin())
				return "Only admins can submit to cryo";
		try {
			// I know FW already does this, but just to be SURE (maybe in less filtered contexts)...
			String obvious = CryoFunctions.checkWellFormedCreature(pm);
			if (obvious != null)
				return obvious;
			// Make a copy of the creature
			LinkedList<PRAYBlock> converted = PRAYBlock.copyList(pm);
			if (!CryoFunctions.creatureConvertInPlace(converted, "DSEX"))
				return "Failure to convert to DSEX";
			PRAYBlock convertedRoot = CryoFunctions.findCreatureRootBlock(converted);
			// Grab the moniker
			String moniker = CryoFunctions.monikerFromRootBlock(convertedRoot);
			// Verify it just to be absolutely sure, because we will be WRITING FILES using this
			// In theory this is a total waste of time as checkWellFormedCreature should have done this
			if (!CreatureDataVerifier.verifyMoniker(moniker))
				return "invalid moniker";
			CryoFunctions.cryoUpdateRootStorage(convertedRoot, who.getUIN());
			return performActualSubmit(who.getUINString() + "." + moniker, PRAYBlock.write(converted, config.messages));
		} catch (Exception ex) {
			log(ex);
			return "Exception: " + ex.toString();
		}
	}

	private synchronized String performActualSubmit(String fn, byte[] write) {
		if (storage.readFromCryo(fn) != null)
			return "Already stored in cryo";
		if (storage.getCryogenicsUsage() + write.length > config.cryo.cryoQuotaBytes.getValue())
			return "Would overflow cryo quota";
		if (!storage.writeToCryo(fn, write))
			return "Failed to write to cryo";
		return null;
	}

	public synchronized LockedFN getFNAndLock(String arg, INatsueUserData receiver) {
		if (!lockedFNs.contains(arg))
			return lockedFNByFNInSync(arg, receiver);
		return null;
	}

	public synchronized LockedFN getRandomFNAndLock(INatsueUserData receiver) {
		LinkedList<String> options = storage.getFilesInCryo();
		options.removeAll(lockedFNs);
		while (!options.isEmpty()) {
			int sz = options.size();
			int opt = rnd.nextInt(sz);
			String optV = options.remove(opt);
			// alright, try it
			LockedFN res = lockedFNByFNInSync(optV, receiver);
			if (res != null)
				return res;
		}
		// we failed
		return null;
	}

	private LockedFN lockedFNByFNInSync(String optV, INatsueUserData receiver) {
		try {
			byte[] data = storage.readFromCryo(optV);
			LinkedList<PRAYBlock> pb = PRAYBlock.read(PacketReader.wrapLE(data), config.messages);
			// do this check early b/c otherwise it turns into an exception in rcc
			PRAYBlock root = CryoFunctions.findCreatureRootBlock(pb);
			if (root == null) {
				log("can't get: " + optV + ", no root");
				return null;
			}
			// check compatibility
			if (CryoFunctions.receiptCompatibilityCheck(pb, receiver) != null)
				return null;
			// compatible, do the following:
			// 1. convert to warp
			if (!CryoFunctions.creatureConvertInPlace(pb, "warp")) {
				log("can't get: " + optV + ", convert failure");
				return null;
			}
			// 2. tag as retrieved from cryo
			CryoFunctions.cryoUpdateRootRetrieval(root, receiver.getUIN());
			// 3. lock
			lockedFNs.add(optV);
			// 4. return in wrapper so it can be unlocked & deleted
			return new LockedFN(optV, pb);
		} catch (Exception e) {
			// now how'd that happen?
			log(e);
		}
		// we failed
		return null;
	}

	public class LockedFN implements AutoCloseable {
		public final String fn;
		public final LinkedList<PRAYBlock> content;
		public LockedFN(String f, LinkedList<PRAYBlock> data) {
			fn = f;
			content = data;
		}
		public void doDelete() {
			synchronized (CryoFrontend.this) {
				storage.deleteFromCryo(fn);
			}
		}
		@Override
		public void close() {
			synchronized (CryoFrontend.this) {
				lockedFNs.remove(fn);
			}
		}
	}

	public synchronized void runSystemCheck(StringBuilder sb) {
		sb.append("-- Cryogenics --\n");
		sb.append("All FNs:");
		for (String s : storage.getFilesInCryo()) {
			sb.append("+ " + s);
			if (lockedFNs.contains(s))
				sb.append(" [LOCKED]");
			sb.append("\n");
		}
		sb.append("Quota: " + storage.getCryogenicsUsage() + " / " + config.cryo.cryoQuotaBytes.getValue() + "\n");
	}

	public synchronized boolean testFNExistsRightNow(String arg) {
		return storage.getFilesInCryo().contains(arg);
	}
}
