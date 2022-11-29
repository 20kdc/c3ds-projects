/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.test;

import org.junit.Test;

import rals.expr.RALConstant;

/**
 * Let's just make sure, hmm?
 *
 */
public class CAOSFloatTest {

	@Test
	public void test() {
		// yeah no
		// System.out.println("DF default format: " + (new DecimalFormat().toPattern()));
		float[] cases = {
				// the obvious
				(0),
				(1),
				// subnormals
				(0.0000000000000000000000000000000000000000000014f),
				(0.0000000000000000000000000000000000000000000028f),
				// in the middle
				(1f/30000f),
				(1f/3f),
				(1f/0.00000003f),
				// really really large
				(100000000000000000.0f),
				(100000000000000000000000000000000000000.0f),
				(300000000000000000000000000000000000000.0f),
				(340000000000000000000000000000000000000.0f),
				(340282356779733000000000000000000000000.0f)
		};
		for (int sign = 0; sign < 2; sign++) {
			if (sign == 1) {
				for (float f : cases)
					simpleCase(-f);
			} else {
				for (float f : cases)
					simpleCase(f);
			}
		}
	}

	private void simpleCase(float v) {
		String cf = RALConstant.Flo.toCAOSFloat(v);
		System.out.println("TEST: " + v + " -> " + cf);
		float vRT = Float.valueOf(cf);
		if (vRT != v) {
			System.out.println(" FAILED: LoP, res = " + vRT);
			throw new RuntimeException("Loss of precision at " + v + " -> " + cf);
		}
		if (!cf.contains(".")) {
			System.out.println(" FAILED: no dot");
			throw new RuntimeException("CAOS expects all floats to contain .");
		}
		if (cf.contains("E")) {
			System.out.println(" FAILED: E");
			throw new RuntimeException("CAOS does not understand scientific notation");
		}
		System.out.println(" PASSED");
	}
}
