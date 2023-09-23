# kc3dsbpy

## I want an example file!

Check `project_moon.blend`.

## Where To Find Stuff

Ok, so, since there's a lot of stuff, I've embedded most information into the addon so you can see it when you hover over stuff.

However, you still need to know where that is.

kc3dsbpy has two places where UI is added, both in Properties:

* Scene Properties: This houses the main panel where you perform major operations. The Custom Properties panel also matters: the addon does store some things here.
* Object Properties: This allows you to assign objects as *markers* and set their properties.

## Basic Principle Of Operation

The addon has six functions:

* Change the visibility of objects.
* Rotate Markers.
* Move the camera to Markers.
* Dump a lot of data to custom properties for rigging.
* Convert images to C16.
* Do the above to render multiple skeletons from a single button press.

The addon has something of a "simple to get working, hard to master" attitude in design.

The flexibility required for various models prevented a more "magical" solution from being created.

Each model will have to find solutions to problems that the addon can't directly solve. (Hence, the properties.)

If you know Blender well, the addon will simply do the annoying work of automating the render, and you can continue rigging as you normally would.

Through this manual, you will see references to *VisScript.* VisScript's purpose is to control object visibility.

VisScript is available on any Object, but is pretty much useless unless the Object is going to be rendered.

## The Quick Notes On Norn Part Frames

In short, each part has four yaws: right, left, front, back. Each yaw has four pitches: down, centre, up, further up.

These are ordered from 0 to 15, and make up a single "variant" of a part. The head has 192 frames, so 12 variants. *Variants only matter for facial expressions and egg status.*

It's pretty much impossible to mess up the yaws, but the pitches can be weird, especially if trying to match an existing model. See "How To Align A Part" about this.

The total amount of frames for a Norn is 544. 192 for the head, 96 of these are dummies as part of the "Mouth" part, and then 16 for other parts except that the body has 64.

The exact details of which frames are which matter to QuickNorn and SpriteBuilder. This addon will calculate it all for you and send everything worth mentioning to custom properties, and you don't interact with frames via animation keyframes or anything, so it's really not a problem you need to worry about.

The main things you need to take away from this are:

1. That this all exists in the first place, particularly the custom properties.
2. If you query properties from parts that don't have them, you'll get whatever the last value that went through was.
3. Mouth part makes up 96 dummy frames. Don't bother adding it, use Head instead.

## Markers

Markers can be any Blender Object, but in practice they will either be:

* Mesh/Surface: If the part does not need a particularly fancy rig and you're perfectly fine with the object origin being the point of rotation.
* Empty: For pretty much anything else.

Markers are how the addon interfaces with your model's rigging for the various rotations that need to be produced of each body part. They are *also* how the addon chooses the location to place the camera.

When these two abilities conflict, the usual solution is Copy Rotation/Location constraints.

*Something to be aware of: The addon sets the camera's local position directly to the marker's global position. Parent the camera to something to shift it away from your model.*

## VisScript

The VisScript controls when an object is rendered.
If empty, the object is always rendered.

VisScript breaks down the input in a specific order:

* `A&&B`: A and B (higher precedence)
* `A|B`: A or B
* `A&B`: A and B
* `!A`: not A
* `A=B`: A is equal to literal "B". The value of B is not case-sensitive.
* `()`
* `A`: A is present and not empty, "0", or "0.0".

Instances of the above operators within `()` are ignored until as late as possible.

To understand the precedence, see these examples:

* `A | B & C | D`: `A | (B & C) | D`: OR of multiple branches, with their own individual list of ANDed conditions.
* `part=Head&&happy|sad`: `(part=Head) & (happy | sad)`: Multiple different branches, but with an overarching condition.
* `part=Body&&egg=1|egg=2&&yaw_id=1|yaw_id=-1`: `(part=Body) & ((egg=1) | (egg=2)) & ((yaw_id=1) | (yaw_id=-1))`: Multiple different values being checked, but their valid values are independent of each other, so having to repeat them for each branch would be bad.

VisScripts source their values from the Custom Properties beginning with `kc3dsbpy.`; this means you can add your own custom properties with this prefix to expose your own switches to VisScripts.

Properties are always converted to strings when VisScript evaluates them.

See (WORKINGS.md)[WORKINGS.md] for a hopefully full reference on the custom properties set by the addon.

## General Tips

### Compositing

For BMP export to work properly, you'll need to use compositing nodes to clean up the transparent areas.

### Collections

The addon can see objects in collections, *but doesn't alter their state.*

This is pretty useful, because it means you can use them to manage your models when VisScript would otherwise interfere.

### Customize Your Blender Layout

This will make things much more convenient.

* You can split/join Blender panels by right-clicking on edges.
* You can pin a datablock in a properties panel, and that panel will then show that datablock, not what you have selected.
* Pin a camera datablock and the Scene datablock at the same time.
* Convert an existing breed's images to a PNG image sequence, then tie a background image sequence's Offset property to a driver based on the Frame setting in the scene.

### How To Align A Part

When aligning a part:

* Ensure you've got background images properly setup for easy checks.
* Align it by frame 9 (Front, zero pitch) first, setting marker X and Z.
* Then work on frame 1, adjusting either marker Y or the location of a separate rotator.
* Adjust pitches and make minor tweaks.
* The X marker position of Body, Head, and Tail parts should always be 0. (This feels reasonably obvious...)

Some notes about pitches:

* When in Manual mode, stick to multiples of 5 degrees.
* F0/S0 are almost always 0. Key word "almost". For Body, it's F1/S1 that are 0.
* Keep in mind front and side angles are separate columns for a reason. Some parts are just like that.

It may be sometimes necessary to add a separate "rotator" object, copying rotation from the marker.

In this case things can get complicated, so follow one general rule:

*If there are two attributes that can get you closer to your goal for this frame, but mess up another frame, adjust both by half.*

Rarely, armatures may be required.

### Random Notes

This is just stuff I couldn't really reasonably put into the addon itself...

* Renders are saved to subdirectories of your render output directory.
* The Render button doesn't do the conversion to C16, just the render. The "PNG -> C16" button only does the conversion, not the render.
* The Render Mode is a number so Drivers (and geometry nodes and so on) can read it.
* Automatic pitch is based around what manual pitch calls F0 as the "centre." That is, a multiplier of zero makes all pitches equal to F0.
* If you haven't learned the basics of Drivers yet, you probably should. This is useful when VisScript would be extremely inconvenient (say, to rotate something).
  * Mainly, Single Property drivers (click the `(x)` left of the variable name, then pick the DNA-looking icon) rather than Transform Channel.
  * On a Scene, `["kc3dsbpy.example"]` accesses the custom property `kc3dsbpy.example` which is the VisScript property `example`.
  * Drivers can be set on custom properties. These custom properties, if prefixed with `kc3dsbpy.`, can be accessed from VisScript.
* You should be able to get by on just VisScript and constraints if you're okay with lots of linked objects or duplicates floating around.

