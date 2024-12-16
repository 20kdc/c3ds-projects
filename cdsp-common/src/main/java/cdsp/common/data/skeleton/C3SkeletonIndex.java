/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package cdsp.common.data.skeleton;

import java.util.LinkedList;

import cdsp.common.data.CreaturesFacts;

/**
 * Index in the skeleton fallback system.
 */
public class C3SkeletonIndex extends SkeletonIndex {
	// 0-25 (this isn't a char because I suspect CE is going to invalidate the range soon)
	public final int breedIndex;
	// 0-3
	public final int genusIndex;
	// 0-1
	public final int bsIndex;
	// 0-5
	public final int ageIndex;

	public C3SkeletonIndex(int b, int g, int s, int a) {
		super(Character.toString((char) ('0' + (g + (s * 4)))) + Character.toString((char) ('0' + a)) + CreaturesFacts.C_23_BREED_INDEX[b]);
		if (b < 0 || b >= CreaturesFacts.C_23_BREED_INDEX.length)
			throw new RuntimeException("Breed out of range.");
		if (g < 0 || g >= CreaturesFacts.GENUS.length)
			throw new RuntimeException("Genus out of range.");
		if (s < 0 || s > 1)
			throw new RuntimeException("Sex out of range.");
		if (a < 0 || a > 9)
			throw new RuntimeException("Age out of range.");
		breedIndex = b;
		genusIndex = g;
		bsIndex = s;
		ageIndex = a;
	}

	@Override
	public SkeletonIndex[] getSearchPath() {
		LinkedList<SkeletonIndex> lls = new LinkedList<>();
		for (int gl = genusIndex; gl >= 0; gl--) {
			for (int sXor = 0; sXor < 2; sXor++) {
				for (int bl = breedIndex; bl >= 0; bl--) {
					for (int al = ageIndex; al >= 0; al--) {
						lls.add(new C3SkeletonIndex(bl, gl, bsIndex ^ sXor, al));
					}
				}
			}
			for (int sXor = 0; sXor < 2; sXor++) {
				for (int bl = breedIndex + 1; bl < CreaturesFacts.C_23_BREED_INDEX.length; bl++) {
					for (int al = ageIndex; al >= 0; al--) {
						lls.add(new C3SkeletonIndex(bl, gl, bsIndex ^ sXor, al));
					}
				}
			}
		}
		return lls.toArray(new SkeletonIndex[0]);
	}
}
