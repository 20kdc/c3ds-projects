# Requirements For `NET: WRIT` Usage In Natsue

To allow for interesting CAOS-based multiplayer usecases, and with an improved engine appearing to be on the horizon, `NET: WRIT` support is being tentatively added, with the caveat that the vanilla engine may still be unstable around it.

Natsue will at least _try_ to accept `NET: WRIT` messages now, however.

`NET: WRIT` is a powerful tool. Here's the overview:

* `NET: HEAR "examplechannel"` adds the `TARG` agent to the given "listening channel", here `"examplechannel"`.
* `NET: WRIT "1234+1" "examplechannel" 1000 "ex1" "ex2"` is a remote `MESG WRT+ (...) 1000 "ex1" "ex2"` to the agents of the given user listening on `"examplechannel"`. The parameters may be strings, integers, floats, or null.

Vanilla clients will ignore `NET WRIT`s not intended for them.

However, the ability to cause `MESG WRT+`, even if a dedicated agent, comes with some particularly hairy responsibilities on the part of Natsue.

With this in mind, there are a number of key requirements Natsue enforces on top of `NET: WRIT`. (This is known as the `restrictive` setting of `allowNetWrit`.)

* Attempts to write with the channel types `"system_message"` and `"add_to_contact_book"` will be blocked.
* The message ID must be 2468.

As an agent developer using `NET: WRIT`, you have _responsibilities_ to avoid creating risks for your users and to ensure your usage is reliable.

* Do not rely on `NET: WRIT` being reliable. `NET: WRIT` messages can be lost for every reason that any agent message can be lost, plus networking is networking. Natsue will silently discard `NET: WRIT` messages if the target is not online.
* Always `INST` at the start of your message handler. It is not advisable to drop `INST` for any reason within that handler, as doing so may cause execution to be interrupted or messages to be dropped.
* In such an `INST`ed handler, do not ever call `NET: UNIK` or `NET: ULIN`, even if you know that user to be in the WWR. The server may not have sent back the response yet. **Ignoring this risks freezing the game.**
* Treat all incoming messages as untrusted data.
* Given how the functionality is built, using `NET: WRIT` at all to _send_ messages on vanilla clients is done _at own risk._ **The lagspikes are a known 'feature' of the vanilla implementation.**
