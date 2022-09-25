# ciesetup

*STATUS: Hopefully of use*

ciesetup is a process designed to take one of three files:

+ `dockingstation.run` (MD5: 5a175535e104e6150005c1f3213aebc3 )
+ `dockingstation_195_64.tar.bz2` (MD5: 90a7ab012bbd4530b63c1ed64fb94464 )
+ `dockingstation.run.tar.bz2` (MD5: 6eb67560ece8c887abcc17db702bf7f3 )

And create a more modernized Docking Station archive. (NOTE: These three all contain the same data and all are dsbuild 195, the final build.)

Note that the checksums given here are for quick checking that you have the right file. They're not checked in ciesetup.

This DOES NOT automatically install universal fixes like Vampress's Bramboo Fix / Amaikokonut's Wolf Control Fix / etc.

You're expected to install them yourself if that's what you want.

## Known Issues

1. Audio was always kind of bad in this port. If you're not trying to do high-performance wolfling runs, have you considered `colour-depth-fix`?
2. Error dialog boxes have been reduced to terminal messages for the time being.
3. The `creatures3` script for running Creatures 3 standalone is not yet finished, and Creatures 3 integration in general is not yet tested.

## Usage

1. Confirm you have: `libportaudio2:i386` (or your distribution's equivalent), `portaudio19-dev` (if cloning with Git), `zenity`, `python3`, and GNU `make`.
2. **If you're cloning directly with Git and you have not already run `make ciesetup` at the repository root, go do that now.**
3. Place one of the above described files into the `repo` directory.
4. Run `make` in the `ciesetup` directory. The result should be `repo/unified.tar`.
5. Extract `pkg_engine.tar` and `pkg_dockingstation.tar` to wherever you want to install the game.

## What Went Wrong (i.e. why does this exist?)

In short:

+ Debian distributions dropped GTK1.2
+ The `libSDL-1.2.so.0` they shipped doesn't support ALSA, but this may be a blessing in disguise, because:
+ The game uses a custom `libstdc++` that will break things if the game ever indirectly loads the system libstdc++ for any reason. Loading, say, `libjack` causes this.
+ The launcher will contact an update server which isn't so dead as to cause the launcher to merely fail cleanly or ignore it, but is so dead as to cause it to give an error.
+ The launcher assumes you want a global installation. This "global with user directories" setup went wrong when the Mac version did it, I see no reason to believe it'll be more stable here.

## How Is It Fixed?

+ `LD_PRELOAD` allows overriding or adding functions in the shipped library set without the potentially hazardous side effects of outright replacing the libraries.
+ A dummy (empty) shared library is created to keep the dynamic linker from complaining of missing libraries.
+ The launcher and language selector have been replaced.
+ The GTK1.2 error dialogs have been replaced with the error being printed to the terminal and options being selectable there.
+ Thanks to `dlmopen`, it's possible to create a second "universe" within the same process in which `portaudio` may live, and access it more or less normally. This has been done.

## Future Ideas

+ Given `dlmopen` can bypass literally all symbol resolution problems ever, what about shimming the entirety of `libSDL-1.2` to use `libsdl1.2-compat`?
+ This also applies to SDL_mixer, maybe future versions at least have better mixing.
+ If it's possible to get this onto `libsdl1.2-compat`, it follows it's possible to take the next logical step and use SDL2 advanced message boxes for the error dialogs?

