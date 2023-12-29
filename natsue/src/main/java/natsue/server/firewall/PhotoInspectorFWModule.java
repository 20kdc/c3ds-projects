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

import cdsp.common.data.pray.PRAYBlock;
import cdsp.common.s16.S16Image;
import natsue.data.babel.UINUtils;
import natsue.data.babel.pm.PackedMessage;
import natsue.data.babel.pm.PackedMessagePRAY;
import natsue.log.ILogProvider;
import natsue.log.ILogSource;
import natsue.server.cryo.CryoFunctions;
import natsue.server.hub.ServerHub;
import natsue.server.photo.IPhotoStorage;
import natsue.server.photo.PhotoFunctions;
import natsue.server.userdata.INatsueUserData;

/**
 * Inspects photos. If a problem is detected, the photo may be substituted.
 * Will otherwise upload them.
 */
public class PhotoInspectorFWModule implements IFWModule, ILogSource {
	private final ServerHub serverHub;
	private final boolean strict;
	private final IPhotoStorage photos;

	public PhotoInspectorFWModule(ServerHub serverHub, boolean strict, IPhotoStorage photos) {
		this.serverHub = serverHub;
		this.strict = strict;
		this.photos = photos;
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
			PRAYBlock rootBlock = CryoFunctions.findCreatureRootBlock(pray.messageBlocks);
			if (rootBlock == null)
				return false;
			boolean hasFirstAssociatedPhoto = false;
			for (PRAYBlock block : pray.messageBlocks) {
				if (block.getType().equals("PHOT")) {
					S16Image decoded = PhotoFunctions.ensureValidPhoto(block.data, serverHub.config.photos, this);
					// The strict flag controls overwriting.
					// In either case, the photo won't be attempted to be saved if "weird"...
					if (strict && decoded == null)
						block.data = PhotoFunctions.invalidPhoto.clone();
					// Prepare saving...
					if (decoded != null && !hasFirstAssociatedPhoto) {
						try {
							hasFirstAssociatedPhoto = true;
							BufferedImage bi = decoded.toBI(false);
							int uid = UINUtils.uid(sourceUser.getUIN());
							ByteArrayOutputStream baos = new ByteArrayOutputStream();
							ImageIO.write(bi, "PNG", baos);
							String moniker = CryoFunctions.monikerFromRootBlock(rootBlock);
							photos.setPhotoPNG(moniker, uid, baos.toByteArray());
						} catch (Exception ex) {
							log(ex);
						}
					}
				}
			}
		}
		return false;
	}

}
