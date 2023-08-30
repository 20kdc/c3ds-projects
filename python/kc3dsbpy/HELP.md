# kc3dsbpy

## Scene

Genus: Controls which genus is being rendered.

Breed Slot: Controls which breed slot is being rendered.

Sexes: Controls which sexes are rendered.

Ages: Controls which ages are rendered.

Render Breeds: Renders the breed as selected above.

Setup Frame: Sets up the frame according to settings. Useful for inspecting the render of a specific part.

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

