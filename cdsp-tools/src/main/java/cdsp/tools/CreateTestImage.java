/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package cdsp.tools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import cdsp.common.s16.CS16Format;
import cdsp.common.s16.CS16IO;
import cdsp.common.s16.S16Image;

/**
 * creates ral_test_card.s16
 */
public class CreateTestImage {
	public static void main(String[] args) throws IOException {
		S16Image testCard = new S16Image(256, 256);
		for (int i = 0; i < 65536; i++)
			testCard.pixels[i] = (short) i;
		byte[] data = CS16IO.encode(new S16Image[] {testCard}, CS16Format.S16_RGB565);
		Files.write(new File("../ral/samples/ral_tint_test_card.s16").toPath(), data);
	}
}
