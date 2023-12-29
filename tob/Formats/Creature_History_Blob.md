# Creature History Blob

Format Structure
----------------

* string moniker
* byte hasCreatureState
* if hasCreatureState != 0:
  * int sex
    * 1: Male, 2: Female
  * int genus
    * 0: Norn, 1: Grendel, 2: Ettin, 3: Geat
  * int variant
    * Range of 1-8 (inclusive).
  * int pointMutations
  * int crossoverPoints
* int eventCount
* Event[eventCount] events
  * It is important to note that these are only "solid" events (engine-defined events that the creature was definitely around to see). This is apparently to help prevent desyncs from, i.e. "child was born" events on other computers during warping.
* string name
  * Client enforces a limit on names by the amount of pixels in them - in practice this means a soft limit of about 32 characters should do.
  * The client likes to send this as empty while sending a user text update. If this is an either-or condition or not is unknown, so right now Natsue will assume blank names indicate this condition.
* int hasUserText
* if hasUserText != 0:
  * string userText
    * 7 lines, each up to about ~124 characters, so 896 is a good limit.

It's worth noting that the game will resend only ranges of events at times.

Events
------

* int eventType
  * As in HIST TYPE. Also see "creature_history_event_names".
* int worldTime
  * As in HIST WTIK.
* int ageTicks
  * As in HIST TAGE.
* int unixTime
  * As in HIST RTIM.
* int lifeStage
  * As in HIST CAGE.
* string moniker1
  * As in HIST MON1.
* string moniker2
  * As in HIST MON2.
* string worldName
  * As in HIST WNAM.
* string worldID
  * As in HIST WUID.
* string userID
  * As in HIST NETU.
* int index
  * Appears to uniquely identify an event within a creature's history (i.e. as deduplication).
  * This index is a "solid index". **Solid indexes are distinct from the GLST index / CAOS index.**

Strings/Monikers/User IDs
-------------------------

Strings are encoded here with an int-length-prefix followed by that many bytes, without null terminators.
Monikers, user IDs, world IDs (...) are strings.
