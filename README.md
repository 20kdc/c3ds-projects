# Assorted directory for compatibility fixes, useful tidbits, etc.

## The Obvious Disclaimer

c3ds-projects contributors are in no way affiliated with Creature Labs, this is all unofficial, and it might turn your hair blue.

## Contents

+ caosproxy: specification & tools for OS-independent and easier to access interconnect between tooling & the game
+ ciesetup: CIE setup improvements
+ colour-depth-fix: improves Linux & Mac compatibilty of Docking Station through renderer patching
+ creature-monitor-gd: tool for monitoring creatures that is hopefully more reliable than alternatives
+ tob: Babel protocol documentation attempt
+ natsue: Babel protocol server
+ ral: Experimental language compiling to CAOS

## License

	c3ds-projects - Assorted compatibility fixes & useful tidbits
	Written starting in 2022 by contributors (see CREDITS.txt)
	To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
	You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

Some parts of this repository use third-party dependencies, in particular:

+ creature-monitor-gd uses Godot Engine ( https://godotengine.org/ ) and dependencies thereof.
+ natsue by default is packaged with sqlite-jdbc, mysql-connector-java, and dependencies of these (though these do not enter c3ds-projects binary releases).
+ RAL uses JSON-java ( https://github.com/stleary/JSON-java ) as part of LSP support.

While c3ds-projects is under CC0, these dependencies are mostly not.

+ Information on Godot's licensing is given as COPYING-godot.txt in release archives, in the form roughly as reported by Godot Engine's "Third-party Licenses" panel.
+ Natsue binary builds include sqlite-jdbc, mysql-connector-java, and by extension Google's protobuf library.
+ JSON-java's `LICENSE` file is as follows: `Public Domain.`

