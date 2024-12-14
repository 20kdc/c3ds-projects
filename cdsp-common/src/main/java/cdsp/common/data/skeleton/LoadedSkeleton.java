/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package cdsp.common.data.skeleton;

import java.awt.Point;
import java.util.function.Function;

import cdsp.common.data.DirLookup;
import cdsp.common.s16.S16Image;

/**
 * A creature skeleton.
 */
public final class LoadedSkeleton<I> {
	public final SkeletonDef def;

	private final Object[] loadedPartImages;
	private final ATTFile[] loadedPartATTs;
	private final SkeletonDef.Part[] loadedParts;

	/**
	 * Loads the skeleton.
	 */
	public LoadedSkeleton(DirLookup src, Function<S16Image, I> converter, SkeletonDef def) {
		this.def = def;
		loadedPartImages = new Object[def.length];
		loadedPartATTs = new ATTFile[def.length];
		loadedParts = new SkeletonDef.Part[def.length];
		for (int i = 0; i < def.length; i++) {
			SkeletonDef.Part part = def.getPart(i);
			loadedParts[i] = part;
			//loadedPartImages[i] = src.findBreedFile(null, suffix, false);
			//loadedPartATTs[i] = ;
		}
	}

	@SuppressWarnings("unchecked")
	public I getPartImage(int i) {
		return (I) loadedPartImages[i];
	}

	public ATTFile getPartATT(int i) {
		return loadedPartATTs[i];
	}

	public int getPartJointX(Point[] partLocations, int[] partDirections, int part, int joint) {
		return partLocations[part].x + loadedPartATTs[part].getX(partDirections[part], joint);
	}

	public int getPartJointY(Point[] partLocations, int[] partDirections, int part, int joint) {
		return partLocations[part].y + loadedPartATTs[part].getY(partDirections[part], joint);
	}

	/**
	 * Updates a skeleton. Beware: The points within are mutated, not just replaced.
	 */
	public void updateSkeleton(Point[] partLocations, int[] partDirections) {
		for (int i = 0; i < partLocations.length; i++) {
			SkeletonDef.Part part = loadedParts[i];
			if (part.parentIndex == -1)
				continue;
			int baseX = getPartJointX(partLocations, partDirections, part.parentIndex, part.parentJoint);
			int baseY = getPartJointY(partLocations, partDirections, part.parentIndex, part.parentJoint);
			baseX -= loadedPartATTs[i].getX(partDirections[i], part.localParentJoint);
			baseY -= loadedPartATTs[i].getY(partDirections[i], part.localParentJoint);
			partLocations[i].x = baseX;
			partLocations[i].y = baseY;
		}
	}
}
