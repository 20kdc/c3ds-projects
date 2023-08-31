# kc3dsbpy

## Scene

This is divided into three rough sections.

### Rendering

Template: The template controls image sizes and age scaling. This is here so if someone needs to crowbar in a new template, they can.

BMP: Outputs `CAxxxx.bmp` files, for use with QuickNorn or SpriteBuilder

Genus: Controls which genus is being rendered.

Slot: Controls which breed slot is being rendered.

Sexes: Controls which sexes are rendered.

Ages: Controls which ages are rendered. Each digit in this text is a single age. Beware: Templates don't universally support all ages.

Pixels Per Unit: This is essentially the mapping from Blender Units to pixels. You can test this using Setup Frame.

Render: Renders the breed as selected above. Note that conversion to C16 is *not* performed by this button, only PNG and BMP.

Renders are saved to subdirectories of your render output directory.

Mode: Arbitrary number, which is written to a Custom Property. As such, you can use this field to quickly control VisScripts/etc. This has to be a number so that it can be read by Drivers.

### PNG to C16

Dither C16 Colour: Dithers colours using 2x2 Bayer. Makes C16 conversion take much longer and probably isn't noticable.

Dither C16 Alpha: Dithers alpha using 2x2 Bayer. You *probably* don't want to do this unless your breed has transparency.

PNG -> C16: Converts PNG files to C16 files. Importantly, this works with the same set of targets as breed rendering.

### Inspection

This section is useful for inspecting the render of a specific part.

Sex: Controls which sex is being rendered.

Age: Controls which age is being rendered.

Frame: Controls which skeleton frame (`x` of `CAxxxx.bmp`) is being rendered.

Setup Frame: Sets up the frame according to settings.

Revert Frame: Undoes visibility changes made during Setup Frame. (warning: If you press this after making changes or if you didn't setup a frame, you'll probably hose whatever you were trying to restore)

## Object

### Marker

Markers control the camera location for parts.
They're also rotated, so parent meshes to them.
(For simple cases, set the mesh as the marker.)

There can only be one marker per part, and parts with no marker aren't rendered (left blank).

Marker doesn't control visibility, VisScript does.

### VisScript

The VisScript controls when an object is rendered.
If empty, the object is always rendered.

* has operator precedence
* `A|B`: A or B
* `A&B`: A and B
* `!A`: not A
* `A=B`: A is equal to literal "B"
* `A`: A is present and not empty or "0"

VisScripts source their values from the Custom Properties beginning with `kc3dsbpy.`; this means you can add your own custom properties with this prefix to exposre your own switches to VisScripts.

See WORKINGS.md for a hopefully full reference on the custom properties set by the addon.

### Into VisScript

The "Into VisScript" button just templates a VisScript of the form `part=ThePartSetAsMarker`.

