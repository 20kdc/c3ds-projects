# Hypercalls

Hypercalls are non-standard outbound PRAY data intended to be interpreted by the server.

This is as opposed to the usual operation of the firewall, which is simply to protect vanilla client CAOS.

Hypercalls are sent as PRAY messages with a single block of type `N@SU`, block name `"natsue_hypercall"`.

Some hypercalls are sent to specific targets, others are sent to the system user (`1+2`).

The string key `"Type"` indicates their purpose.

In versions of Natsue where hypercalls are supported, on connection, the server will always send the following NET: WRIT:

* From: Yourself
* Channel: `"natsue_version"`
* Message ID: `2468`
* Param 1: The API version (presently `1`)
* Param 2: The Natsue version, as a string

## `"writ"`

* `"Type" "writ"`
* Purpose: Workaround for broken engine functionality; sending NET: WRIT from vanilla engine
* Target: Whoever the original target of the NET: WRIT is
* Introduced: API 1

This PRAY chunk is translated into a `NET: WRIT` and resent.

Keep in mind the `NetWrit.md` requirements.

String Keys:

* `"Channel"` : `NET: WRIT` channel
* `"Param1 String"` : `_p1_` (as a string)
* `"Param1 Float"` : `_p1_` (as a float)
* `"Param2 String"` : `_p2_` (as a string)
* `"Param2 Float"` : `_p2_` (as a float)

Int Keys:

* `"Message"` : `NET: WRIT` message ID. Optional, will default to 2468.
* `"Param1 Int"` : `_p1_` (as an int)
* `"Param2 Int"` : `_p2_` (as an int)

Example:

```
"en-GB"

group N@SU "natsue_hypercall"

"Type" "writ"
"Channel" "system_message"
"Message" 2469
"Param1 String" "Sending a system message to oneself"
```

`outv net: make 1 "hvtest.txt" net: user va00`
