/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package cdsp.common.data.skeleton;

import java.awt.Point;
import java.io.File;
import java.io.IOException;

import cdsp.common.data.DirLookup;
import cdsp.common.data.DirLookup.Location;
import cdsp.common.s16.CS16IO;
import cdsp.common.s16.S16Image;

/**
 * A creature skeleton.
 */
public final class LoadedSkeleton {
	public static final LoadedSkeleton EMPTY = new LoadedSkeleton(SkeletonDef.EMPTY);

	public final SkeletonDef def;

	private final S16Image[][] loadedPartImages;
	private final ATTFile[] loadedPartATTs;
	private final SkeletonDef.Part[] loadedParts;

	/**
	 * Empty skeleton of the given type.
	 */
	public LoadedSkeleton(SkeletonDef def) {
		this.def = def;
		loadedPartImages = new S16Image[def.length][];
		loadedPartATTs = new ATTFile[def.length];
		loadedParts = new SkeletonDef.Part[def.length];
		for (int i = 0; i < def.length; i++) {
			SkeletonDef.Part part = def.getPart(i);
			loadedParts[i] = part;
		}
	}

	/**
	 * Loads the skeleton.
	 */
	public LoadedSkeleton(DirLookup src, SkeletonIndex[] genes, SkeletonDef def) throws IOException {
		this(def);
		for (int i = 0; i < def.length; i++) {
			SkeletonDef.Part part = loadedParts[i];
			SkeletonIndex[] searchPath = genes[part.sourceGene].getSearchPath();
			try {
				File image = SkeletonIndex.findFileIn(Location.IMAGES, src, searchPath, part.id, ".c16");
				if (image == null) {
					System.err.println("NOENT @ " + searchPath[0] + " part " + part.id + " c16");
				} else {
					loadedPartImages[i] = image != null ? CS16IO.decodeCS16(image) : null;
				}
			} catch (Exception ex) {
				System.err.println("Error @ " + searchPath[0] + " part " + part.id + " c16");
				ex.printStackTrace();
			}
			try {
				File att = SkeletonIndex.findFileIn(Location.BODY_DATA, src, searchPath, part.id, ".att");
				if (att == null) {
					System.err.println("NOENT @ " + searchPath[0] + " part " + part.id + " att");
				} else {
					loadedPartATTs[i] = att != null ? new ATTFile(att) : null;
				}
			} catch (Exception ex) {
				System.err.println("Error @ " + searchPath[0] + " part " + part.id + " att");
				ex.printStackTrace();
			}
		}
	}

	public S16Image getPartImage(int i, int j) {
		S16Image[] frames = loadedPartImages[i];
		if (frames == null)
			return null;
		if (j < 0 || j > frames.length)
			return null;;
		return frames[j];
	}

	public ATTFile getPartATT(int i) {
		return loadedPartATTs[i];
	}

	public int getPartJointX(Point[] partLocations, int[] partFrames, int part, int joint) {
		return partLocations[part].x + loadedPartATTs[part].getX(partFrames[part] % def.poseDirCount, joint);
	}

	public int getPartJointY(Point[] partLocations, int[] partFrames, int part, int joint) {
		return partLocations[part].y + loadedPartATTs[part].getY(partFrames[part] % def.poseDirCount, joint);
	}

	/**
	 * Updates a skeleton. Beware: The points within are mutated, not just replaced.
	 */
	public void updateSkeleton(Point[] partLocations, int[] partFrames) {
		for (int i = 0; i < partLocations.length; i++) {
			SkeletonDef.Part part = loadedParts[i];
			if (part.parentIndex == -1)
				continue;
			int baseX = getPartJointX(partLocations, partFrames, part.parentIndex, part.parentJoint);
			int baseY = getPartJointY(partLocations, partFrames, part.parentIndex, part.parentJoint);
			baseX -= loadedPartATTs[i].getX(partFrames[i] % def.poseDirCount, part.localParentJoint);
			baseY -= loadedPartATTs[i].getY(partFrames[i] % def.poseDirCount, part.localParentJoint);
			partLocations[i].x = baseX;
			partLocations[i].y = baseY;
		}
	}
}
