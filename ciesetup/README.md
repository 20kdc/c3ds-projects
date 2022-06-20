# ciesetup

*STATUS: Incomplete But Theoretically Useful*

ciesetup is a process designed to take one of three files:

+ `dockingstation.run` (MD5: 5a175535e104e6150005c1f3213aebc3 )
+ `dockingstation_195_64.tar.bz2` (MD5: 90a7ab012bbd4530b63c1ed64fb94464 )
+ `dockingstation.run.tar.bz2` (MD5: 6eb67560ece8c887abcc17db702bf7f3 )

And create a more modernized Docking Station archive. (NOTE: These three all contain the same data and all are dsbuild 195, the final build.)

Note that the checksums given here are for quick checking that you have the right file. They're not checked in ciesetup.

Also note this WILL NOT handle universal fixes like Vampress's Bramboo Fix / Amaikokonut's Wolf Control Fix / etc.

## Usage

The `ciesetup` directory should be extracted to somewhere which will act as your workspace for the conversion.

Place one of the aforementioned input files into the `repo` directory.

Then run `make` in the `ciesetup` directory. Python 3 is expected.

Make drives the process, while the individual short Python scripts perform specific transformations.

The following are some particular runtime dependencies:

+ Zenity is expected for language selection (and possibly in future error dialogs).
+ `libportaudio2:i386` is expected for patched audio support

