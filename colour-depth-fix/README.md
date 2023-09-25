# colour depth fix

This is designed to avert the 16-bit colour problem without using Xephyr (slow and potentially not available on Mac).

This version should work with:

* Docking Station engine "2.286"
* Creatures 3 engine "1.162"
* Creatures 3 engine "1.158"
* Creatures 3 engine "1.147"

Note that it will NOT do checksumming or anything like that, to avoid interference with other patches.
However it does check that the bytes it's replacing are what it expects them to be, and this should be enough safety.

See below section for ideas in regards to C3/etc.

## Application instructions

1. Backup your existing engine.exe
2. Run bin/ddrhk.exe (if it's missing, you just got a source release and you need to compile it)
3. Select the engine.exe in your Docking Station directory
4. Copy bin/ddrhk.dll into your Docking Station directory
5. See if it works

## Hexeditable settings

This is just here in case anyone *really* needs it.

There are settings which can be modified by hex-editing the binary.

`LimitWindowXY:Y` tries to avoid the window being placed off-screen. This may, however, act in an unwanted way on multi-monitor setups.

It can thus be disabled by changing the `Y` to `N`.

Also, imagine there's a convenient blank space here on the manual with `5678` printed, as if by a different printer.

## Technical Summary

ddrhk.c -> ddrhk.exe: injector

You can also simply patch the import table yourself, DirectDrawCreate -> DirectDrawHooked & DDRAW.dll -> DDRHK.dll

dscf.c & dscf_a.asm -> ddrhk.dll: the actual hook DLL

This patches the CreateSurface calls to go to elsewhere which patches them to have a 565 pixel format
I HAVE attempted to shim DirectDraw, this was really unstable and not a good idea
The use of an external DLL avoids having to do anything too complex

An additional benefit of the patch is that (presently for DS only) it prevents errors due to the window being made too small if the window was minimized or such when closing the game.

## Alternatives

elishacloud's DxWrapper: https://github.com/elishacloud/dxwrapper/wiki/Creatures-2%3A-The-Albian-Years

Yes, I'm aware that's for Creatures 2, but it works just as well for Creatures 3.

But there's a problem, you need to go into `winecfg` and set the ddraw DLL to `native, builtin`. This may not be ideal, especially if you ended up with a native Windows `ddraw.dll`.

### Where has the "Using the Docking Station engine on C3" section gone?

~~This one's simpler - copy `Catalogue/vocab constructs.catalogue` from Docking Station to Creatures 3, and copy Docking Station's `engine.exe` across.~~

*This method* is hilariously unstable in the face of Steam updates or literally any error or circumstance that calls upon catalogues that changed with DS.

Perform at your own risk.

In theory a more comprehensive catalogue fix could be created, as the necessary patching is in the purview of `ciesetup`. Maybe this time with something to make it more override-y to stop the previous issues.
