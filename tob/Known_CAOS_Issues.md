# Known CAOS Issues

*This file is a result of the Natsue Project written for reference by two groups:*

1. Natsue and external developers who wish to solve the issues.

2. Curious people.

This is for issues with the Docking Station Bootstrap (maybe you want [Known Engine Issues](Known_Engine_Issues.md)?). That is, there's the potential to work around these issues with (in order of preference):

1. Natsue server intervention to prevent the issues from occurring.

2. CAOS patches.

## Floating Norns

Docking Station, when importing a creature, does not make any guarantees about their `ATTR` or `BHVR` values. This is rather easy to solve, as IIRC, reference copies of the creature `ATTR`/`BHVR` values have been `GAME` variables since C3. It's much harder to actually get corrected scripts into DS, though.

## Portal/Export Crashes

In short, if a creature uses a portal and is then exported before the creature leaves, the game will have an error as the portal attempts to do stuff to a creature that no longer exists. (This is actually the reason the RAL documentation recommends `inst` and checking your agent variables for `null` after any form of delay.)

## Workshop Screen Error

If you attempt to warp a creature from the Workshop, but an error occurs, clicking the creature's face in the lower panel will cause an error. Cause not fully understood.

## Verm's NB Norns

In short, Norns with a classifier of `4 1 -1`. **They crash whoever receives them if not prepared, so Natsue blocks these unless you specifically enable a flag on your account.*

## The Wasteland Glitch

In short, unknown CA for the category of Geats causes corruption in scent adjustment in `APPR`. For long versions check Creatures Wiki or I probably left the really *detailed* notes in the CAOS Coding Cave. *Natsue blocks geats unless you specifically enable a flag on your account.*

# 

## What If Creature But Evil?

This is included here more for FAQ purposes about what counts as a CAOS bug.

Everything I have seen about Docking Station implies that the Warp was meant to be "anarchy within limits". Don't break people's computers and don't break their worlds in any way that isn't "they let in a murderer and got surprised when the murderer went and did murder". The training dummy exists, and you're allowed to make genetic abominations (just like everyone else). If you think there should be more diagnostic tools, write them, and people might choose to use them.
