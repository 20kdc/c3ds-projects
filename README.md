# Assorted directory for compatibility fixes, useful tidbits, etc.

## The Obvious Disclaimer

c3ds-projects contributors are in no way affiliated with Creature Labs, this is all unofficial, and it might turn your hair blue.

## Contents

+ aquarium: Reliability testing tool
+ assorted-caos: Random dumping ground for CAOS
+ caosproxy: specification & tools for OS-independent and easier to access interconnect between tooling & the game
+ cdsp-common: Java library used by Natsue and RAL
+ cdsp-tools: A place further Java-based tools may go at some point
+ ciesetup: CIE setup improvements (Now containing Docker!)
+ colour-depth-fix: improves Linux & Mac compatibilty of Docking Station through renderer patching
+ creature-monitor-gd: tool for monitoring creatures that is hopefully more reliable than alternatives
+ efw-integration: Integration assets for the eem.foo server (severely "work in progress" i.e. there's literally nothing there right now)
+ natsue: Babel protocol server
+ tob: Babel protocol documentation attempt
+ ral: 'Experimental' language compiling to CAOS
+ rust: Rust libraries/etc. for this repository go here.

## License

	c3ds-projects - Assorted compatibility fixes & useful tidbits
	Written starting in 2022 by contributors (see CREDITS.txt)
	To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
	You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

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
