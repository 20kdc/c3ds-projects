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
	public final int dirCount, poseCount, poseDirCount, geneCount;
	private final String[] genes;
	private final Part[] parts;
	private final int[][] zOrders;
	public final int length;

	public static final SkeletonDef EMPTY = new SkeletonDef(1, 1, new String[] {"All"}, new Part[] {
		new Part("b", -1, -1, -1, 1, 0),
	}, new int[][] {
		new int[] {0}
	});

	public static final SkeletonDef C3 = new SkeletonDef(4, 4, new String[] {
			"Head",
			"Body",
			"Legs",
			"Arms",
			"Tail",
			"Hair"
		}, new Part[] {
		//  0 Body    PI  PJ LPJ  FR SR
		new Part("b", -1, -1, -1, 12, 1),
		//  1 Head
		new Part("a",  0,  0,  0,  4, 0),
		//  2 Leg Upper L
		new Part("c",  0,  1,  0,  1, 2),
		//  3 Leg Lower L
		new Part("d",  2,  1,  0,  1, 2),
		//  4 Foot L
		new Part("e",  3,  1,  0,  1, 2),
		//  5 Leg Upper R
		new Part("f",  0,  2,  0,  1, 2),
		//  6 Leg Lower R
		new Part("g",  5,  1,  0,  1, 2),
		//  7 Foot R
		new Part("h",  6,  1,  0,  1, 2),
		//  8 Arm Upper L
		new Part("i",  0,  3,  0,  1, 3),
		//  9 Arm Lower L
		new Part("j",  8,  1,  0,  1, 3),
		// 10 Arm Upper R
		new Part("k",  0,  4,  0,  1, 3),
		// 11 Arm Lower R
		new Part("l", 10,  1,  0,  1, 3),
		// 12 Tail Base
		new Part("m",  0,  5,  0,  1, 4),
		// 13 Tail Tip
		new Part("n", 12,  1,  0,  1, 4)
	}, new int[][] {
		// Right
		{
				8, 9,
				2, 3, 4,
				0, 1,
				12, 13,
				10, 11,
				5, 6, 7,
		},
		// Left
		{
				10, 11,
				5, 6, 7,
				0, 1,
				12, 13,
				8, 9,
				2, 3, 4,
		},
		// Front
		{
				13, 12,
				2, 3, 4, 5, 6, 7,
				0, 1,
				10, 11,
				8, 9,
		},
		// Back
		{
				10, 11,
				2, 3, 4, 5, 6, 7,
				8, 9,
				0, 1,
				12, 13,
		}
	});

	/**
	 * Creates a skeleton.
	 * Be aware: Parts cannot have parents after themselves.
	 */
	public SkeletonDef(int dc, int pc, String[] rc, Part[] p, int[][] zOrders) {
		this.dirCount = dc;
		this.poseCount = pc;
		this.geneCount = rc.length;
		this.genes = rc;
		this.poseDirCount = dc * pc;
		this.zOrders = zOrders;
		parts = p;
		length = p.length;
	}

	public Part getPart(int i) {
		return parts[i];
	}

	/**
	 * Gets an array of skeleton appearance gene names.
	 */
	public String[] getGenes() {
		return genes.clone();
	}

	/**
	 * Gets a gene name.
	 */
	public String getGeneName(int index) {
		return genes[index];
	}

	public void updateZOrder(int[] partFrames, int[] zOrder) {
		int dir = (partFrames[0] / poseCount) % dirCount;
		//System.out.println(zOrder.length + " " + partFrames.length + " " + zOrders[dir].length + " " + dir);
		System.arraycopy(zOrders[dir], 0, zOrder, 0, zOrders[dir].length);
	}

	public static final class Part {
		public final String id;
		public final int parentIndex, parentJoint, localParentJoint, sourceGene;
		public final int frameCount;

		public Part(String id, int pi, int pj, int lpj, int frameCount, int sourceGene) {
			this.id = id;
			parentIndex = pi;
			parentJoint = pj;
			localParentJoint = lpj;
			this.sourceGene = sourceGene;
			this.frameCount = frameCount;
		}
	}
}
