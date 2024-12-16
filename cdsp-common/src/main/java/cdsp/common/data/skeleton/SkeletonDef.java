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
	private final PoseChar[] poseChars;
	private final String[] genes;
	private final Part[] parts;
	private final int[][] zOrders;
	public final int length;
	public final String defaultPoseString;
	public final int poseStringLength;

	public static final SkeletonDef EMPTY = new SkeletonDef(1, 1, new PoseChar[] {}, new String[] {"All"}, new Part[] {
		new Part("b", -1, -1, -1, 0),
	}, new int[][] {
		new int[] {0}
	});

	public static final SkeletonDef C3 = new SkeletonDef(4, 4, new PoseChar[] {
		new C3DirectionChar(),
		new C3PoseChar("Head", '1', 1),
		new C3PoseChar("Body", '2', 0),
		new C3PoseChar("LegLU", '2', 2),
		new C3PoseChar("LegLL", '1', 3),
		new C3PoseChar("FootL", '2', 4),
		new C3PoseChar("LegRU", '2', 5),
		new C3PoseChar("LegRL", '1', 6),
		new C3PoseChar("FootR", '2', 7),
		new C3PoseChar("ArmLU", '0', 8),
		new C3PoseChar("ArmLL", '1', 9),
		new C3PoseChar("ArmRU", '1', 10),
		new C3PoseChar("ArmRL", '2', 11),
		new C3PoseChar("TailRoot", '0', 12),
		new C3PoseChar("TailTip", '0', 13)
	}, new String[] {
		"Head",
		"Body",
		"Legs",
		"Arms",
		"Tail",
		"Hair"
	}, new Part[] {
		//  0 Body    PI  PJ LPJ  SR FR
		new Part("b", -1, -1, -1,  1, new String[] {"1", "2", "3", "4"}),
		//  1 Head
		new Part("a",  0,  0,  0,  0, new String[] {"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12"}),
		//  2 Leg Upper L
		new Part("c",  0,  1,  0,  2),
		//  3 Leg Lower L
		new Part("d",  2,  1,  0,  2),
		//  4 Foot L
		new Part("e",  3,  1,  0,  2),
		//  5 Leg Upper R
		new Part("f",  0,  2,  0,  2),
		//  6 Leg Lower R
		new Part("g",  5,  1,  0,  2),
		//  7 Foot R
		new Part("h",  6,  1,  0,  2),
		//  8 Arm Upper L
		new Part("i",  0,  3,  0,  3),
		//  9 Arm Lower L
		new Part("j",  8,  1,  0,  3),
		// 10 Arm Upper R
		new Part("k",  0,  4,  0,  3),
		// 11 Arm Lower R
		new Part("l", 10,  1,  0,  3),
		// 12 Tail Base
		new Part("m",  0,  5,  0,  4),
		// 13 Tail Tip
		new Part("n", 12,  1,  0,  4)
	}, new int[][] {
		// Right
		{
				8, 9, // arm L
				2, 3, 4, // leg L
				0, // body
				12, 13, // tail
				10, 11, // arm R
				5, 6, 7, // leg R
				1, // head
		},
		// Left
		{
				10, 11, // arm R
				5, 6, 7, // leg R
				0, // body
				12, 13, // tail
				8, 9, // arm L
				2, 3, 4, // leg L
				1, // head
		},
		// Front
		{
				13, 12, // tail
				2, 3, 4, 5, 6, 7, // legs
				8, 9, 10, 11, // arms
				0, 1, // body/head
		},
		// Back
		{
				2, 3, 4, 5, 6, 7, // legs
				1, 0, // head/body
				8, 9, 10, 11, // arms
				12, 13, // tail
		}
	});

	/**
	 * Creates a skeleton.
	 * Be aware: Parts cannot have parents after themselves.
	 */
	public SkeletonDef(int dc, int pc, PoseChar[] poseChars, String[] rc, Part[] p, int[][] zOrders) {
		this.dirCount = dc;
		this.poseCount = pc;
		this.poseChars = poseChars;
		char[] poseString = new char[poseChars.length];
		for (int i = 0; i < poseString.length; i++)
			poseString[i] = poseChars[i].def;
		this.defaultPoseString = new String(poseString);
		this.poseStringLength = poseString.length;
		this.geneCount = rc.length;
		this.genes = rc;
		this.poseDirCount = dc * pc;
		this.zOrders = zOrders;
		parts = p;
		length = p.length;
	}

	public final PoseChar[] getPoseChars() {
		return poseChars.clone();
	}

	public final void decodePoseString(int[] partFrames, String pose) {
		int poseLength = pose.length(); 
		for (int i = 0; i < poseLength; i++) {
			if (i >= poseChars.length)
				break;
			poseChars[i].apply(this, partFrames, pose.charAt(i));
		}
	}

	public final void setState(int[] partFrames, int index, int state) {
		int part = partFrames[index];
		partFrames[index] = (state * poseDirCount) + (part % poseDirCount);
	}

	public final void setDir(int[] partFrames, int index, int dir) {
		int part = partFrames[index];
		int pose = part % poseCount;
		part /= poseCount;
		part -= part % dirCount;
		part += dir;
		part *= poseCount;
		part += pose;
		partFrames[index] = part;
	}

	public final void setPose(int[] partFrames, int index, int pose) {
		int part = partFrames[index];
		part -= part % poseCount;
		part += pose;
		partFrames[index] = part;
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
		private final String[] states;

		public Part(String id, int pi, int pj, int lpj, int sourceGene) {
			this(id, pi, pj, lpj, sourceGene, new String[] { "none" });
		}
		public Part(String id, int pi, int pj, int lpj, int sourceGene, String[] states) {
			this.id = id;
			parentIndex = pi;
			parentJoint = pj;
			localParentJoint = lpj;
			this.sourceGene = sourceGene;
			this.states = states;
		}

		public String[] getStates() {
			return states.clone();
		}
	}

	public static abstract class PoseChar {
		public final String name, values;
		public final char def;
		public final int associatedPartIndex;

		public PoseChar(String name, String values, char def, int api) {
			this.name = name;
			this.values = values;
			this.def = def;
			this.associatedPartIndex = api;
		}

		public char[] getValues() {
			return values.toCharArray();
		}

		public boolean hasValue(char chr) {
			return values.indexOf(chr) != -1;
		}

		public abstract void apply(SkeletonDef parent, int[] partFrames, char pose);
	}

	public static class C3DirectionChar extends PoseChar {
		private static final int[] remap = {3, 2, 0, 1};

		public C3DirectionChar() {
			super("Dir.", "X?!0123", '1', -1);
		}

		@Override
		public void apply(SkeletonDef parent, int[] partFrames, char pose) {
			if (pose < '0' || pose > '3')
				return;
			for (int i = 0; i < partFrames.length; i++)
				parent.setDir(partFrames, i, remap[pose - '0']);
		}
	}

	public static class C3PoseChar extends PoseChar {
		private final int[] partIndexes;
		public C3PoseChar(String name, char def, int... partIndexes) {
			super(name, "X?!012345", def, partIndexes.length > 0 ? partIndexes[0] : -1);
			this.partIndexes = partIndexes;
		}

		@Override
		public void apply(SkeletonDef parent, int[] partFrames, char pose) {
			if (pose == '4') {
				for (int i = 0; i < partIndexes.length; i++) {
					parent.setDir(partFrames, partIndexes[i], 2);
					parent.setPose(partFrames, partIndexes[i], 1);
				}
			} else if (pose == '5') {
				for (int i = 0; i < partIndexes.length; i++) {
					parent.setDir(partFrames, partIndexes[i], 3);
					parent.setPose(partFrames, partIndexes[i], 1);
				}
			} else {
				if (pose < '0' || pose > '3')
					return;
				for (int i = 0; i < partIndexes.length; i++)
					parent.setPose(partFrames, partIndexes[i], pose - '0');
			}
		}
	}
}
