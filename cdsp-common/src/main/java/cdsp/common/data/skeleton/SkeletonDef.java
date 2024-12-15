/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package cdsp.common.data.skeleton;

/**
 * A skeleton definition.
 */
public final class SkeletonDef {
	public final int dirCount;
	private final Part[] parts;
	public final int length;

	public static final SkeletonDef EMPTY = new SkeletonDef(1, new Part[] {
		new Part("b", -1, -1, -1, 1),
	});

	public static final SkeletonDef C3 = new SkeletonDef(16, new Part[] {
		//  0 Body    PI  PJ LPJ
		new Part("b", -1, -1, -1, 12),
		//  1 Head
		new Part("a",  0,  0,  0,  4),
		//  2 Leg Upper L
		new Part("c",  0,  1,  0,  1),
		//  3 Leg Lower L
		new Part("d",  2,  1,  0,  1),
		//  4 Foot L
		new Part("e",  3,  1,  0,  1),
		//  5 Leg Upper R
		new Part("f",  0,  2,  0,  1),
		//  6 Leg Lower R
		new Part("g",  5,  1,  0,  1),
		//  7 Foot R
		new Part("h",  6,  1,  0,  1),
		//  8 Arm Upper L
		new Part("i",  0,  3,  0,  1),
		//  9 Arm Lower L
		new Part("j",  8,  1,  0,  1),
		// 10 Arm Upper R
		new Part("k",  0,  4,  0,  1),
		// 11 Arm Lower R
		new Part("l", 10,  1,  0,  1),
		// 12 Tail Base
		new Part("m",  0,  5,  0,  1),
		// 13 Tail Tip
		new Part("n", 12,  1,  0,  1)
	});

	/**
	 * Creates a skeleton.
	 * Be aware: Parts cannot have parents after themselves.
	 */
	public SkeletonDef(int dc, Part[] p) {
		this.dirCount = dc;
		parts = p;
		length = p.length;
	}

	public Part getPart(int i) {
		return parts[i];
	}

	public static final class Part {
		public final String id;
		public final int parentIndex, parentJoint, localParentJoint;
		public final int frameCount;

		public Part(String id, int pi, int pj, int lpj, int frameCount) {
			this.id = id;
			parentIndex = pi;
			parentJoint = pj;
			localParentJoint = lpj;
			this.frameCount = frameCount;
		}
	}
}
