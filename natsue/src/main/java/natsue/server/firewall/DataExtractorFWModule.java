/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.firewall;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

import cdsp.common.data.pray.ExportedCreatures;
import cdsp.common.data.pray.PRAYBlock;
import cdsp.common.s16.S16Image;
import natsue.data.babel.pm.PackedMessage;
import natsue.data.babel.pm.PackedMessagePRAY;
import natsue.log.ILogProvider;
import natsue.log.ILogSource;
import natsue.server.glst.IGLSTStorage;
import natsue.server.hub.ServerHub;
import natsue.server.photo.IPhotoStorage;
import natsue.server.photo.PhotoFunctions;
import natsue.server.userdata.INatsueUserData;

/**
 * Inspects photos. If a problem is detected, the photo may be substituted.
 * Will otherwise upload them.
 */
public class DataExtractorFWModule implements IFWModule, ILogSource {
	private final ServerHub serverHub;
	private final boolean strict;
	private final IPhotoStorage photos;
	private final IGLSTStorage glst;

	public DataExtractorFWModule(ServerHub serverHub, boolean strict, IPhotoStorage photos, IGLSTStorage glst) {
		this.serverHub = serverHub;
		this.strict = strict;
		this.photos = photos;
		this.glst = glst;
	}

	@Override
	public ILogProvider getLogParent() {
		return serverHub;
	}

	@Override
	public void wwrNotify(boolean online, INatsueUserData userData) {
	}

	@Override
	public boolean handleMessage(INatsueUserData sourceUser, INatsueUserData destUser, PackedMessage message) {
		if (message instanceof PackedMessagePRAY) {
			PackedMessagePRAY pray = (PackedMessagePRAY) message;
			// Also verifies moniker as basic part of operation.
			PRAYBlock rootBlock = ExportedCreatures.findCreatureRootBlock(pray.messageBlocks);
			if (rootBlock == null)
				return false;
			String moniker = ExportedCreatures.monikerFromRootBlock(rootBlock);
			for (PRAYBlock block : pray.messageBlocks) {
				if (block.getType().equals("GLST")) {
					glst.storeGLST(moniker, block.data);
				} else if (block.getType().equals("PHOT")) {
					S16Image decoded = PhotoFunctions.ensureValidPhoto(block.data, serverHub.config.photos, this);
					int eventIndex = ExportedCreatures.getPHOTEventIndex(block.getName(), moniker, rootBlock.getType());
					// The strict flag controls overwriting.
					// In either case, the photo won't be attempted to be saved if "weird"...
					if (strict && decoded == null)
						block.data = PhotoFunctions.invalidPhoto.clone();
					// Prepare saving...
					if (decoded != null && eventIndex != -1 && serverHub.config.photos.photosEnabled.getValue()) {
						// prevent duplication
						if (!photos.shouldPhotoExist(moniker, eventIndex)) {
							try {
								BufferedImage bi = decoded.toBI(false);
								ByteArrayOutputStream baos = new ByteArrayOutputStream();
								ImageIO.write(bi, "PNG", baos);
								photos.setPhoto(moniker, eventIndex, sourceUser.getUIN(), baos.toByteArray(), decoded.width, decoded.height);
							} catch (Exception ex) {
								log(ex);
							}
						}
					}
				}
			}
		}
		return false;
	}

}
