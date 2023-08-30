# kc3dsbpy

## Marker

Markers control the camera location for parts.
They're also rotated, so parent meshes to them.
(For simple cases, set the mesh as the marker.)

There can only be one marker per part, and parts with no marker aren't rendered (left blank).

Marker doesn't control visibility, VisScript does.

## VisScript

The VisScript controls when an object is rendered.
If empty, the object is always rendered.


* has operator precedence
* `A|B`: A or B
* `A&B`: A and B
* `!A`: not A
* `A=B`: A is equal to literal "B"
* `A`: A is present and not empty or "0"


