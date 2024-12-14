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
	private final Part[] parts;
	public final int length;

	public static final SkeletonDef C3 = new SkeletonDef(new Part[] {
		//  0 Body                               R  L  F  B
		new Part("b", -1, -1, -1, 12, new int[] {0, 0, 0, 0}),
		//  1 Head
		new Part("a",  0,  0,  0,  4, new int[] {0, 0, 0, 0}),
		//  2 Leg Upper L
		new Part("c",  0,  0,  0,  1, new int[] {0, 0, 0, 0}),
		//  3 Leg Lower L
		new Part("d",  2,  0,  0,  1, new int[] {0, 0, 0, 0}),
		//  4 Foot L
		new Part("e",  3,  0,  0,  1, new int[] {0, 0, 0, 0}),
		//  5 Leg Upper R
		new Part("f",  0,  0,  0,  1, new int[] {0, 0, 0, 0}),
		//  6 Leg Lower R
		new Part("g",  5,  0,  0,  1, new int[] {0, 0, 0, 0}),
		//  7 Foot R
		new Part("h",  6,  0,  0,  1, new int[] {0, 0, 0, 0}),
		//  8 Arm Upper L
		new Part("i",  0,  0,  0,  1, new int[] {0, 0, 0, 0}),
		//  9 Arm Lower L
		new Part("j",  8,  0,  0,  1, new int[] {0, 0, 0, 0}),
		// 10 Arm Upper R
		new Part("k",  0,  0,  0,  1, new int[] {0, 0, 0, 0}),
		// 11 Arm Lower R
		new Part("l", 10,  0,  0,  1, new int[] {0, 0, 0, 0}),
		// 12 Tail Base
		new Part("m",  0,  0,  0,  1, new int[] {0, 0, 0, 0}),
		// 13 Tail Tip
		new Part("n", 12,  0,  0,  1, new int[] {0, 0, 0, 0})
	});

	/**
	 * Creates a skeleton.
	 * Be aware: Parts cannot have parents after themselves.
	 */
	public SkeletonDef(Part[] p) {
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
		private final int[] zOrders;

		public Part(String id, int pi, int pj, int lpj, int frameCount, int[] zOrders) {
			this.id = id;
			parentIndex = pi;
			parentJoint = pj;
			localParentJoint = lpj;
			this.frameCount = frameCount;
			this.zOrders = zOrders;
		}

		public int getZOrder(int idx) {
			if (idx < 0 || idx >= zOrders.length)
				return 0;
			return zOrders[idx];
		}
	}
}
