# colour depth fix

This is designed to avert the 16-bit colour problem without using Xephyr (slow and potentially not available on Mac).

This version is rated ONLY for use with the Docking Station engine "2.286".
Usage with Creatures 3 "1.147" or "1.162" WILL FAIL. (It should be possible to implement, though.)
Note that it will NOT do checksumming or anything like that, to avoid interference with other patches.
However it does check that the bytes it's replacing are what it expects them to be, and this should be enough safety.

See below section for ideas in regards to C3/etc.

## Application instructions

1. Backup your existing engine.exe
2. Run bin/ddrhk.exe (if it's missing, you just got a source release and you need to compile it)
3. Select the engine.exe in your Docking Station directory
4. Copy bin/ddrhk.dll into your Docking Station directory
5. See if it works

## Technical Summary

ddrhk.c -> ddrhk.exe: injector

You can also simply patch the import table yourself, DirectDrawCreate -> DirectDrawHooked & DDRAW.dll -> DDRHK.dll

dscf.c & dscf_a.asm -> ddrhk.dll: the actual hook DLL

This patches the CreateSurface calls to go to elsewhere which patches them to have a 565 pixel format
I HAVE attempted to shim DirectDraw, this was really unstable and not a good idea
The use of an external DLL avoids having to do anything too complex

## Creatures 3 and so forth

colour-depth-fix doesn't support other versions of Creatures 3 at present, but solutions exist that should work.

### DxWrapper

My recommendation is to use elishacloud's DxWrapper: https://github.com/elishacloud/dxwrapper/wiki/Creatures-2%3A-The-Albian-Years

Yes, I'm aware that's for Creatures 2, but it works just as well for Creatures 3.

But there's a problem, you need to go into `winecfg` and set the ddraw DLL to `native, builtin`. This may not be ideal, especially if you ended up with a native Windows `ddraw.dll`.

### Using the Docking Station engine on C3

This one's simpler - copy `Catalogue/vocab constructs.catalogue` from Docking Station to Creatures 3, and copy Docking Station's `engine.exe` across.

