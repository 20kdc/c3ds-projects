/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;

import rals.parser.*;
import rals.tooling.Injector;
import rals.types.ScriptIdentifier;

/**
 * The RAL compiler.
 * Date of arguably being a compiler: Middle of the night between 29th and 30th of September, 2022.
 */
public class Main {
	public static void main(String[] args) throws IOException {
		if (args.length < 1) {
			printHelp();
			return;
		}
		if (args[0].equals("compile") || args[0].equals("compileInstall") || args[0].equals("compileEvents") || args[0].equals("compileRemove")) {
			if (args.length != 3) {
				printHelp();
				return;
			}
			File outFile = new File(args[2]);
			IncludeParseContext ic = Parser.run(args[1]);
			StringBuilder outText = new StringBuilder();
			if (args[0].equals("compile")) {
				ic.module.compile(outText, ic.typeSystem);
			} else if (args[0].equals("compileInstall")) {
				ic.module.compileInstall(outText, ic.typeSystem);
			} else if (args[0].equals("compileEvents")) {
				ic.module.compileEvents(outText, ic.typeSystem);
			} else if (args[0].equals("compileRemove")) {
				ic.module.compileRemove(outText, ic.typeSystem);
			} else {
				throw new RuntimeException("?");
			}
			FileOutputStream fos = new FileOutputStream(outFile);
			for (char chr : outText.toString().toCharArray())
				fos.write(chr);
			fos.close();
		} else if (args[0].equals("inject") || args[0].equals("injectEvents") || args[0].equals("injectRemove")) {
			if (args.length != 2) {
				printHelp();
				return;
			}
			IncludeParseContext ic = Parser.run(args[1]);
			LinkedList<String> queuedRequests = new LinkedList<>();
			if (args[0].equals("inject")) {
				// events
				ic.module.compileEventsForInject(queuedRequests, ic.typeSystem);
				// install
				StringBuilder outText = new StringBuilder();
				outText.append("execute\n");
				ic.module.compileInstall(outText, ic.typeSystem);
				queuedRequests.add(outText.toString());
			} else if (args[0].equals("injectEvents")) {
				ic.module.compileEventsForInject(queuedRequests, ic.typeSystem);
			} else if (args[0].equals("injectRemove")) {
				StringBuilder outText = new StringBuilder();
				outText.append("execute\n");
				ic.module.compileRemove(outText, ic.typeSystem);
				queuedRequests.add(outText.toString());
			} else {
				throw new RuntimeException("?");
			}
			for (String req : queuedRequests)
				System.out.println(Injector.cpxRequest(req));
		} else if (args[0].equals("cpxConnectionTest")) {
			// be a little flashy with this
			System.out.println(Injector.cpxRequest("execute\n" + Parser.runCPXConnTest()));
		} else {
			printHelp();
		}
	}

	private static void printHelp() {
		System.out.println("RAL Compiler");
		System.out.println("compile INPUT OUTPUT: Compiles INPUT and writes CAOS to OUTPUT");
		System.out.println("compileInstall INPUT OUTPUT: Same as compile, but only the install script");
		System.out.println("compileEvents INPUT OUTPUT: Same as compile, but only the event scripts");
		System.out.println("compileRemove INPUT OUTPUT: Same as compile, but only the remove script (without rscr prefix!)");
		System.out.println("inject INPUT: Injects event scripts and install script");
		System.out.println("injectEvents INPUT: Injects event scripts only");
		System.out.println("injectRemove INPUT: Injects removal script");
		System.out.println("cpxConnectionTest: Test CPX connection");
	}
}
