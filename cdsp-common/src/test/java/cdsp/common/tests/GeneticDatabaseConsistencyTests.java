/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package cdsp.common.tests;

import static org.junit.Assert.*;

import org.junit.Test;

import cdsp.common.data.genetics.GenVersion;

public class GeneticDatabaseConsistencyTests {
	@Test
	public void test() {
		for (GenVersion version : GenVersion.VERSIONS) {
			for (int type : version.getGeneTypes()) {
				assertNotNull("version [" + version + "] genetype " + Integer.toHexString(type) + " layout absent", version.getGeneLayout(type));
			}
		}
	}
}
