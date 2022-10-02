/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import rals.code.*;
import rals.parser.*;
import rals.types.*;

/**
 * The RAL compiler.
 * Date of arguably being a compiler: Middle of the night between 29th and 30th of September, 2022.
 */
public class Main {
	public static void main(String[] args) throws IOException {
		if (args.length < 2) {
			System.err.println("PATHS... INITIAL OUTFILE");
			return;
		}
		TypeSystem ts = new TypeSystem();
		Module m = new Module();
		String init = args[args.length - 2];
		String outFile = args[args.length - 1];
		File[] searchPaths = new File[args.length - 2];
		for (int i = 0; i < args.length - 2; i++)
			searchPaths[i] = new File(args[i]);
		Parser.parseFile(ts, m, searchPaths, "std/compiler_helpers.ral");
		Parser.parseFile(ts, m, searchPaths, init);
		StringBuilder outText = new StringBuilder();
		m.compile(outText, ts);
		FileOutputStream fos = new FileOutputStream(outFile);
		for (char chr : outText.toString().toCharArray())
			fos.write(chr);
		fos.close();
	}
}
