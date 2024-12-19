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

import cdsp.common.app.DoWhatIMeanLoader;
import cdsp.common.data.genetics.GenPackage;

/**
 * Gene concatenator, meow~
 */
public class GeneCat {
	public static void main(String[] args) throws IOException {
		if (args.length == 0) {
			System.out.println("genecat <GEN inputs...> [-o <GEN output>]");
			System.out.println("");
			System.out.println("I've attempted* to make this compatible with all Creatures genome versions.");
			System.out.println("* Attempted is the key word...");
			System.out.println("Genus is inherited from the first GEN file; you can use one with just a Genus gene if you want.");
			System.out.println("If an input filename ends in .creature, genecat will  ");
			System.out.println("");
			System.out.println("This was thrown together at the final hour.");
			System.out.println("Listening to a beautiful song about Noelle from Deltarune (9-kS3jEzVeg) as I write this.");
			System.out.println("Everything's all complicated, and I'm going to be doing something difficult soon.");
			System.out.println("And it's scary, you know? I'd rather just ramble about it in this code, where it might not be seen.");
			System.out.println("I know I phase in and out of communities a lot.");
			System.out.println("So I probably haven't been as active in Creatures areas as I could be.");
			System.out.println("I hope I've managed to bring something useful this year.");
			System.out.println("This is my first CCSF submission, so... I'd say I'm nervous about it, but... I think I have worse to worry about.");
			System.out.println("So I'm actually kinda cheerful, which is weird! I don't understand how I feel right now.");
			System.out.println(" ~ 20kdc, 19th December 2024");
			System.exit(1);
		}
		GenPackage working = null;
		boolean output = false;
		File outputFile = null;
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-o")) {
				output = true;
			} else {
				File f = new File(args[i]);
				if (output) {
					outputFile = f;
				} else if (working == null) {
					working = DoWhatIMeanLoader.loadGeneticsCore(f);
				} else {
					GenPackage next = DoWhatIMeanLoader.loadGeneticsCore(f);
					working = working.cat(next);
				}
			}
		}
		if (working == null) {
			System.err.println("You passed exactly zero GEN files; we don't even know what version of GEN you want, nevermind genus!");
			System.exit(1);
		}
		byte[] finale24CCSFBranch = working.toFileData();
		if (outputFile != null) {
			Files.write(outputFile.toPath(), finale24CCSFBranch);
		} else {
			System.out.write(finale24CCSFBranch);
		}
	}
}
