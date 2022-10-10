# RAL Tooling Reference

The RAL compiler is a Java command-line application, and therefore is run via `java -jar cdsp-ral-0.666-SNAPSHOT.jar`.

It is designed for use in simple build systems, and injection (for rapid iteration similar to CAOS Tool)

## Subcommands

### compile

The `compile` subcommand, and it's friends `compileDebug`, `compileInstall`, `compileEvents`, and `compileRemove`, make up the principal method of writing RAL scripts.

The parameters are the RAL file to compile, and the output filename.

The usual variant is a 1:1 compilation of a RAL file to an equivalent Bootstrap-style file (install script, then event scripts, then remove script).

The `compileInstall` variant only compiles the install script, the `compileEvents` script only compiles the event scripts, and the `compileRemove` script only compiles the remove script (without the `rscr` header, so this is suitable for agent remove scripts).

`compileDebug` is `compile` but it writes additional metadata.

### inject

The `inject` subcommand, and it's friends `injectEvents` and `injectRemove`, perform a CAOS injection over CPX (which requires having `caosprox` running).

The sole parameter is the RAL file to inject.

`inject` injects the event scripts and the install script, while `injectEvents` and `injectRemove` inject only the event scripts and the remove script respectively.

### cpxConnectionTest

The `cpxConnectionTest` subcommand is used to test injection.

## Standard Library Location

In JAR form, the RAL compiler expects the `include` directory to be nearby (either in the same directory as the JAR file or in the immediate parent directory).

To override this, the environment variable `RAL_STDLIB_PATH` may be set.
