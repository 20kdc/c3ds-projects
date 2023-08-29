* Database: "Gizmo tree", takes in dictionaries, creates annotated dictionaries
* GizmoNode: Node that ultimately contributes to Gizmo Frame List
* Gizmo Frame List: List of dictionaries describing frames. Frames can be "real" or "fake". Fake frames are blank, they just make QuickNorn happy.
* Gizmo: Stored scene reconfiguration parameters to prepare for the *actual* render
* Controller: `__init__.py`

## ALL PROPS

* C: Read by Controller
* G: Read by Gizmo
* U: Userflag, Gizmo is intended to in future forward these to corresponding Empties for user driver control
* D: Read by Database

now for the props

* "caid": D: Object used by Database to allocate the actual CAxxxx.bmp files in the proper order.
* "real": C: As opposed to fake. Fake frames have this set to false and filepath set but nothing else.
* "filepath": CD: In anything but a frame, sort of a "current working directory" input. In frames, the final filepath to put this BMP file into.
* "pitch", "yaw": G: Controls part rotation.
* "part_name": G: Object ID for part anchor, prefix for part vis.
* "frame": G: Frame of specific body part (facial expressions etc.) 1-based.
* "size": G: Size of image.
* "male": U: 1 if male, 0 otherwise
* "female": U: 1 if female, 0 otherwise
* "age": U: age number
* "scale": UG: age scale control value

