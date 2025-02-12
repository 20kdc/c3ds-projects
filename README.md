# Assorted directory for compatibility fixes, useful tidbits, etc.

## The Obvious Disclaimer

c3ds-projects contributors are in no way affiliated with Creature Labs, this is all unofficial, and it might turn your hair blue.

## Contents

+ aquarium: Reliability testing tool
+ assorted-caos: Random dumping ground for CAOS
+ caosproxy: specification & tools for OS-independent and easier to access interconnect between tooling & the game
+ cdsp-common: Java library used by Natsue and RAL
+ cdsp-tools: A place for further Java-based tools
+ ciesetup: CIE setup improvements (Now containing Docker!)
+ colour-depth-fix: improves Linux & Mac compatibilty of Docking Station through renderer patching
+ creature-monitor-gd: tool for monitoring creatures that is hopefully more reliable than alternatives
+ efw-integration: Integration assets for the eem.foo server (severely "work in progress" i.e. there's literally nothing there right now)
+ natsue: Babel protocol server
+ tob: Babel protocol documentation attempt
+ ral: 'Experimental' language compiling to CAOS
+ rust: Rust libraries/etc. for this repository go here.

## Where to put things and how to write them

* CLI
	* CLI tools with performance or external dependency requirements should be written in Rust and go in the `rust` section.
	* Otherwise, most CLI tools should be written in Python and go in the `python` section.
* GUI
	* GUI tools should be written in Java. Godot 3.x is an option but is costly build-time-wise.
		* The underlying library code goes in `cdsp-common` for use across the Java projects.
* caosprox, ciesetup
	* Code specifically meant to interface with native code in a specific environment should be written in whatever way reduces install dependencies. In practice, this means that it's written in C and compiled only for the target(s). Wine or other such layers are expected to be used as necessary.
* Blender, age/breed DB, etc.
	* Anything that needs to touch Blender has to be written in Python, as native dependencies are untenable.
	* The age/breed DB solely exists in Python right now. It might be an idea to move it to something else, but for now this is what you get; the alternatives seem to be worse in various important ways. Unironically considering a TSV file.
* Common Code
	* All common Java code goes in `cdsp-common`.
	* All common Python code goes in `python`.
	* Common Rust code mostly goes in `libkc3ds`, except `norncli`.

## License

	c3ds-projects - Assorted compatibility fixes & useful tidbits
	Written starting in 2022 by contributors (see CREDITS.txt)
	To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
	You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

Some parts of this repository directly import third-party dependencies:

+ `umvn.class`, `umvn` and `umvn.cmd` are from https://github.com/20kdc/gabien-common/tree/master/micromvn ; a copy of the Unlicense is included in each file.

Some parts of this repository use third-party dependencies (which are not included in the source of this repository), in particular:

+ creature-monitor-gd uses Godot Engine ( https://godotengine.org/ ) and dependencies thereof.
+ natsue by default is packaged with sqlite-jdbc, mysql-connector-java, and dependencies of these (though these do not enter c3ds-projects binary releases).
+ All Java projects in this repository use JSON-java ( https://github.com/stleary/JSON-java )
	* RAL uses it for LSP support.
	* `cdsp-common` and by extension `cdsp-tools` uses it for game directory configuration.
+ The libraries and programs written in Rust use Rust and the Rust Standard Library. They also use some external libraries including `image`.

While c3ds-projects is under CC0, these dependencies are mostly not.

+ Information on Godot's licensing is given as COPYING-godot.txt in release archives, in the form roughly as reported by Godot Engine's "Third-party Licenses" panel.
+ Natsue binary builds include sqlite-jdbc, mysql-connector-java, and by extension Google's protobuf library.
+ JSON-java's `LICENSE` file is as follows: `Public Domain.`
+ _Binaries for the Rust components are not and will not be compiled or shipped at the current time due to licensing issues, see <https://github.com/rust-lang/rust/issues/67014>_
