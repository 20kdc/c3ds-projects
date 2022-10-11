# NetWrits

`NET: WRIT` messages, described in [C2E Message](../Formats/C2E_Message.md), are not used often at the high-level.

They are used in particular as a mechanism for the server to send something quick and simple to the client.

Why they were not used further probably has something to do with *the unresolved crash bug in the `NET: WRIT` command implementation.*

(Current Natsue unofficial server versions provide a great way to exhibit this bug. Not that you probably wanted to do that...)

As such, the messages you're *intended* to use all perform checks that the `NET: WRIT` came from the client that's receiving the `NET: WRIT`. The server of course can (and presumably does) trivially create this condition artificially.

## "system_message" Channel

This channel is for global notifications.

Message 2469 with the usual "only from the receiver" logic.

`_p1_` is the text, while `_p2_` is ignored.

## "add_to_contact_book" Channel

This channel is so that the website (you know, the one that doesn't exist) can add users to your contact book.

The intended way to use this channel is with message 2468, with the usual "only from the receiver" logic.

`_p1_` is the contact in question, while `_p2_` is ignored.

Scripts 135, 137, 138, and 1000 are internal, at least in theory - it is not known (and potentially not knowable) if this represented a bypass for the checks in practice.


