# Known Engine Issues

_This file is a result of the Natsue Project written for reference by two groups:_

1. Natsue and external developers who wish to solve the issues.

2. Curious people.

This is for issues inherent to the `engine-netbabel.dll` implementation of NetBabel (maybe you want [Known CAOS Issues](Known_CAOS_Issues.md)?). That is, these issues can at best be worked around with (in order of preference):

1. Careful server-side manuvering.

2. CAOS patches.

3. Defining new "workaround" protocols server-side and writing CAOS patches to use them.

4. Binary patching (this is probably not acceptable).

Which options are applicable will be listed.

## NET: WRIT / Virtual Circuits

The `NET: WRIT` command is broken. The Creature Labs developers knew it was broken, and despite there being a simple way to fix it (send the messages the same way as PRAY messages), this fix was not actually applied for one reason or another.

### Virtual Circuits

Virtual Circuits are the functionality used for `NET: WRIT`. 

Their ideology is to provide an equivalent API to sockets regardless of if the transport is a direct peer-to-peer connection (Natsue would never allow this due to IP leak and port forwarding issues) or if the data is routed through the Natsue server.

The problem is that they're effectively a broken implementation of a TCP-like protocol with somewhat more reliability requirements of the underlying transport and somewhat less safety against unreliability issues like "the client disconnected". In fact if the target is missing a Virtual Circuit connection can freeze the game, which is the big risk of `NET: WRIT`.

### Workarounds

1. Just don't use `NET: WRIT` and don't rely on the feature. This is the current Natsue policy, but see next.

2. Custom PRAY-based protocol that is translated into a `NET: WRIT` on the other end. Natsue will implement this when someone comes up with a use-case for it - this will be the first case of Natsue defining an extension to the NetBabel protocol.

3. A binary patch for this is very feasible. Natsue will route `NET: WRIT` messages sent via the route used for PRAY messages if you can send them.

4. Natsue could attempt to work around the bugs with Virtual Circuits on it's end. This is still risky however as if the connection to the server fails during transmission it's quite possible for the game to simply seize up without an avenue of recovery.

## WWR / GetConnectionInfo seize-up

When adding a user to your contact list, the game can freeze. This can be easily replicated by use of `NET: WHOZ`, `NET: WHON` and `NET: ULIN` with a user you do not have as a contact. Simply applying these commands repetitively (the *speed* matters) for a few seconds is enough to crash the client.

It has been determined the crashing operation is `NET: ULIN`.

### Workarounds

1. Since this only really happens on contact adding, it may be enough to politely request any user to expect crashes on `contact`.

2. It might be an idea to add the contact as the user comes online. System's self-add doesn't seem to cause client crashes. Perhaps store it in the spool?

3. It is possible that the *exact order of operations* in the client CAOS is incorrect, and that if that order was corrected, crashes would become impossible or effectively impossible.

4. It may simply be a matter of outlawing `NET: ULIN` use. It can't crash if it's never run.

## Creature Obliteration, Part A

Connection interruption during receipt can potentially kill a creature.

### Workarounds

1. Natsue makes a very careful and calculated use of the Virtual Circuit system when transferring any data deemed to be important (basically, creatures and mail). The purpose of this is that the Virtual Circuit system has a _guaranteed, engine level_ response to an incoming Virtual Circuit connection. If Virtual Circuit does not respond, the message probably didn't arrive, or at least it cannot be said to be known it has arrived - otherwise, if it does respond, that the message arrived is certain. If the user or the CAOS deletes it immediately afterwards, that is not Natsue's problem. If the message didn't arrive, it'll be resent when the user next connects. The worst possible case here is creature duplication, but Natsue cannot make any security or reliability assumption that a creature is not locally duplicated anyway.

2. Blame the scary and dangerous nature of the Warp.

## Creature Obliteration, Part B

A local crash can delete creatures received from another user.

### Workarounds

1. It is impossible for Natsue to make any regulations on what an end user's computer does with creatures, including `QUIT` or crash deletion. However, a CAOS patch could be created to avoid immediately deleting creatures on import, instead simply marking them in some way to lock them to the world and keeping them around until the next successful save/load cycle (or transmission).

2. Alternatively, a CAOS patch may simply `SAVE` whenever a creature has been received. This leads to a lot of redundant saving.

# 
