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
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;

import rals.cctx.CodeWriter;
import rals.code.*;
import rals.debug.*;
import rals.diag.DiagRecorder;
import rals.parser.*;
import rals.tooling.*;
import rals.tooling.raljector.RALjector;

/**
 * The RAL compiler.
 * Date of arguably being a compiler: Middle of the night between 29th and 30th of September, 2022.
 * Date of actually running any code in-game: 3rd October, 2022.
 */
public class Main {
	public static void main(String[] args) throws IOException {
		// IMPORTANT: All text printed before we go into LSP stdio mode needs to be on STDERR.
		System.err.println("RAL Compiler");
		String override = System.getenv("RAL_STDLIB_PATH");
		if ((override != null) && (override.equals("")))
			override = null;
		// Attempt to find the RAL standard library.
		File ralStandardLibrary = new File("include");
		if (override != null) {
			ralStandardLibrary = new File(override);
		} else {
			try {
				URL myURL = Main.class.getClassLoader().getResource("rals/Main.class");
				String f = myURL.getFile();
				int splitIdx = f.indexOf('!');
				if (splitIdx != -1)
					f = f.substring(0, splitIdx);
				URL myURL2 = new URL(f);
				File ralJarDir = new File(myURL2.getPath()).getParentFile();
				ralStandardLibrary = new File(ralJarDir, "include");
				// target dir.
				if (!ralStandardLibrary.isDirectory())
					ralStandardLibrary = new File(ralJarDir.getParentFile(), "include");
			} catch (Exception ex) {
				// that's fine
			}
		}
		System.err.println("Standard Library Directory: " + ralStandardLibrary);
		if (!ralStandardLibrary.isDirectory()) {
			System.err.println("Warning! Directory is missing. A directory called 'include' should be at or near the RAL jar file.");
			System.err.println("Failing this, specify RAL_STDLIB_PATH in your environment.");
		}
		IDocPath stdLibDP = new FileDocPath(ralStandardLibrary);
		// the rest!
		if (args.length < 1) {
			printHelp();
			return;
		}
		if (args[0].equals("compile") ||
				args[0].equals("compileDebug") ||
				args[0].equals("compileInstall") ||
				args[0].equals("compileEvents") ||
				args[0].equals("compileRemove")) {
			if (args.length != 3) {
				printHelp();
				return;
			}
			File outFile = new File(args[2]);
			IncludeParseContext ic = Parser.run(stdLibDP, new File(args[1]));
			StringBuilder outText = new StringBuilder();
			OuterCompileContext cctx = new OuterCompileContext(outText, new CommentingDebugRecorder(false));
			OuterCompileContext cctxDbg = new OuterCompileContext(outText, new FullDebugRecorder());
			Scripts resolvedCode = ic.module.resolve(ic.typeSystem, ic.diags, ic.hcm);
			if (args[0].equals("compile")) {
				resolvedCode.compile(cctx);
			} else if (args[0].equals("compileDebug")) {
				resolvedCode.compile(cctxDbg);
			} else if (args[0].equals("compileInstall")) {
				resolvedCode.compileInstall(cctx);
			} else if (args[0].equals("compileEvents")) {
				resolvedCode.compileEvents(cctx);
			} else if (args[0].equals("compileRemove")) {
				resolvedCode.compileRemove(cctx);
			} else {
				throw new RuntimeException("?");
			}
			unwrapCalmly(ic.diags);
			FileOutputStream fos = new FileOutputStream(outFile);
			fos.write(outText.toString().getBytes(CodeWriter.CAOS_CHARSET));
			fos.close();
			System.out.println("Compile completed");
		} else if (args[0].equals("inject") || args[0].equals("injectInstall") || args[0].equals("injectEvents") || args[0].equals("injectRemove")) {
			if (args.length != 2) {
				printHelp();
				return;
			}
			StringBuilder sb = new StringBuilder();
			boolean ok = false;
			DummyDebugRecorder ddr = new DummyDebugRecorder();
			if (args[0].equals("inject")) {
				ok = inject(sb, stdLibDP, new File(args[1]), ddr, ScriptSection.Events, ScriptSection.Install);
			} else if (args[0].equals("injectInstall")) {
				ok = inject(sb, stdLibDP, new File(args[1]), ddr, ScriptSection.Install);
			} else if (args[0].equals("injectEvents")) {
				ok = inject(sb, stdLibDP, new File(args[1]), ddr, ScriptSection.Events);
			} else if (args[0].equals("injectRemove")) {
				ok = inject(sb, stdLibDP, new File(args[1]), ddr, ScriptSection.Remove);
			} else {
				throw new RuntimeException("?");
			}
			System.out.print(sb.toString());
			if (!ok)
				System.exit(1);
		} else if (args[0].equals("cpxConnectionTest")) {
			// be a little flashy with this
			System.out.println(Injector.cpxRequest("execute\n" + Parser.runCPXConnTest(stdLibDP)));
		} else if (args[0].equals("lsp")) {
			new LSPBaseProtocolLoop(new LanguageServer(ralStandardLibrary, false), false).run();
		} else if (args[0].equals("lspLog")) {
			FileOutputStream fos = new FileOutputStream(new File(ralStandardLibrary, "lsp.log"), true);
			System.setErr(new PrintStream(fos, true, "UTF-8"));
			new LSPBaseProtocolLoop(new LanguageServer(ralStandardLibrary, true), true).run();
		} else if (args[0].equals("lspLoud")) {
			new LSPBaseProtocolLoop(new LanguageServer(ralStandardLibrary, true), true).run();
		} else if (args[0].equals("docGen")) {
			IncludeParseContext ic = Parser.run(stdLibDP, new File(args[1]));
			// We have to run the resolve here so that macros pre-compile (so we can mine out their types).
			// But we don't actually care for the contents of the resolved output.
			ic.module.resolve(ic.typeSystem, ic.diags, ic.hcm);
			StringBuilder sb = new StringBuilder();
			DocGen.Rule[] rules = new DocGen.Rule[args.length - 2];
			// insert a rule by default NOT including stdlib
			rules[0] = new DocGen.Rule("-std/");
			for (int i = 3; i < args.length; i++)
				rules[i - 2] = new DocGen.Rule(args[i]);
			DocGen.build(sb, ic, rules);
			FileOutputStream fos = new FileOutputStream(args[2]);
			fos.write(sb.toString().getBytes(StandardCharsets.UTF_8));
			fos.close();
		} else if (args[0].equals("raljector")) {
			new RALjector(stdLibDP);
		} else {
			printHelp();
		}
	}

	public static boolean inject(StringBuilder sb, IDocPath stdLibDP, File f, IDebugRecorder di, ScriptSection... sections) {
		try {
			IncludeParseContext ic = Parser.run(stdLibDP, f);
			LinkedList<String> queuedRequests = new LinkedList<>();
			Scripts resolvedCode = ic.module.resolve(ic.typeSystem, ic.diags, ic.hcm);
			for (ScriptSection s : sections)
				resolvedCode.compileSectionForInject(queuedRequests, di, s);
			String res = ic.diags.unwrapToString();
			if (res != null) {
				sb.append("Compile failed:\n");
				sb.append(res);
				return false;
			}
			for (String req : queuedRequests) {
				sb.append(Injector.cpxRequest(req));
				sb.append("\n");
			}
			return true;
		} catch (Exception ex) {
			exceptionIntoSB(sb, ex);
			return false;
		}
	}

	public static void exceptionIntoSB(StringBuilder sb, Exception ex) {
		sb.append("-- Error --\n");
		StringWriter sw = new StringWriter();
		ex.printStackTrace(new PrintWriter(sw));
		sb.append(sw);
	}

	private static void unwrapCalmly(DiagRecorder diags) {
		String res = diags.unwrapToString();
		if (res != null) {
			System.out.println("Compile failed:");
			System.out.print(res);
			System.exit(1);
			return;
		}
	}

	private static void printHelp() {
		System.out.println("compile INPUT OUTPUT: Compiles INPUT and writes CAOS to OUTPUT");
		System.out.println("compileDebug INPUT OUTPUT: Same as compile, but with added compiler debug information");
		System.out.println("compileInstall INPUT OUTPUT: Same as compile, but only the install script");
		System.out.println("compileEvents INPUT OUTPUT: Same as compile, but only the event scripts");
		System.out.println("compileRemove INPUT OUTPUT: Same as compile, but only the remove script (without rscr prefix!)");
		System.out.println("inject INPUT: Injects event scripts and install script");
		System.out.println("injectInstall INPUT: Injects install script only");
		System.out.println("injectEvents INPUT: Injects event scripts only");
		System.out.println("injectRemove INPUT: Injects removal script");
		System.out.println("lsp: Language server over standard input/output");
		System.out.println("lspLog: Like lsp, but writes out lsp.log and shows additional LSP debug information");
		System.out.println("lspLoud: Like lsp, but sends debug information to standard error and shows additional LSP debug information");
		System.out.println("docGen INPUT OUTPUT (+/-PREFIX)...: Generates AsciiDoc documentation.");
		System.out.println("cpxConnectionTest: Test CPX connection");
		System.out.println("raljector: RALjector GUI");
	}
}
