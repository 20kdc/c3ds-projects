# Natsue Web API Documentation

***The Natsue Web API is intended for anonymous access requirements, such as online creature geneology tracking. If your use-case requires anything that would require an API key or token or account login, it should go via the NetBabel protocol. See `tob` at the root of this repository for details. And if you're not going to write an auto-rejector, please kindly `denyrandom` your bot account.***

*In addition, the API may not be enabled for public access, except for the photo PNG route which is shared with the web interface. If the web interface is also disabled, then nothing is usable.*

All endpoints have an optional `apiKey` query-string parameter. 

This allows specifying an API key that can optionally be written into the Natsue configuration file. Doing this, should such a key be configured, bypasses the API block.

## Routes

### GET /api/index

Returns an object with server information.

`version`: Server version identifier.

`versionURL`: URL of server codebase.

`pageSizeAPIWorldCreatures`: Maximum entries returned by `/api/worldCreatures` at once.

`pageSizeAPIUserWorlds`: Maximum entries returned by `/api/userWorlds` at once.

### GET /api/usersOnline

Returns an array of User objects.

There is no guarantee everybody in this list is online at the exact moment you receive the list, or while you're processing it. This can even lead to the `online` field being false in the returned list.

### GET /api/creatureInfo?moniker=...

Returns a CreatureInfo object for the given moniker.

### GET /api/creatureEvents?moniker=...

Returns all creature events as an array of CreatureEvent objects.

### GET /api/user?uin=...

Returns a User object for the given UIN.

*Big scary caveat:* UINs are written `uid+hid`, but `+` has a different meaning. URL-encode this field! Or if you must fudge it, `%2B`.

### GET /api/user?nickname=...

Returns a User object for the given nickname.

### GET /api/world?id=...

Given a World ID, returns a World object.

### GET /api/worldCreatures?id=...&offset=...

Given a World ID and an offset into the list, returns an array of up to `pageSizeAPIWorldCreatures` CreatureInfo objects.

Note that the list can change (most likely expand) as you're paging it in. You will have to deal with this.

### GET /api/userWorlds?uin=...&offset=...

Given a UIN and an offset into the list, returns an array of up to `pageSizeAPIUserWorlds` World objects.

Note that the list can change (most likely expand) as you're paging it in. You will have to deal with this.

*Big scary caveat:* UINs are written `uid+hid`, but `+` has a different meaning. URL-encode this field! Or if you must fudge it, `%2B`.

### GET /api/creaturePhotoMetadata?moniker=...&eventIndex=...

Returns a creature life event photo's metadata (CreaturePhotoMetadata).

### GET /creaturePhoto.png?moniker=...&eventIndex=...

Not a typo; `/api` is not given here.

Returns a creature life event photo's content.

*This route can be public even if the API is not. However, this route can also be individually made non-public even if other API routes are public.*

## Object Definitions

### CreatureInfo

`moniker`: Always present. Identifier of the creature.

*The following may not be present if a result can include a creature while not explicitly requiring information for the creature exist. However, the information will always be provided if available.*

`name`: User-given name of the creature.

`userText`: User-given descriptive text. Be aware of Ancestry Tracker lists.

`state`: A CreatureInfoState, immutable data about the creature.

### CreatureInfoState

*All values here are user-provided 32-bit integers and are therefore untrusted.*

`sex`: 1 is male, 2 is female. *Other values can, will and most importantly have occurred.*

`genus`: 0 is Norn, 1 is Grendel, 2 is Ettin, 3 is Geat.

`variant`: Value from 1-8 inclusive. Not of much importance in practice.

`pointMutations`: Number of point mutations during conception.

`crossoverPoints`: Number of crossover points during conception.

### CreatureEvent

*With the exceptions of sender and moniker, this is untrusted data.*

`sender`: User object.

`moniker`: Moniker of the creature whose event this is.

`eventIndex`: Solid Index of the event. Note that this is not necessarily equivalent to the array index, particularly if events go missing.

`eventType`: Type of the event (integer). See CAOS documentation for information on this.

`worldTime`: World time in ticks of the event.

`ageTicks`: Creature's age in ticks when the event occurred.

`unixTime`: Unix time in seconds when the event occurred.

`lifeStage`: Life stage when the event occurred.

`param1`: Event parameter 1. String.

`param2`: Event parameter 2. String.

`world`: World object.

### User

`uin`: UIN string, i.e. `"1+1"`.

`exists`: Relatively obvious boolean.

`online`: Also relatively obvious boolean.

The folllowing fields are not guaranteed to exist (specifically if `exists` is false):

`nickname`: The user's nickname.

`nicknameFolded`: The user's nickname after case folding.

`flags`: The user's flags (see next section)

### World

`id`: World ID.

`name`: World name.

`user`: User relevant to this world object. Note that this isn't necessarily the world's owner, and due to worlds not being explicitly registered (yes, even though the login dialog claims that's what you're doing), there is no *guarantee* of which owner is true even for the world route.

### CreaturePhotoMetadata

`moniker`: Moniker.

`eventIndex`: Creature life event index. _**This is according to the GLST/CAOS index, not the 'Solid'/Warp index.**_

`senderUIN`: UIN string. _A User object is not used here as the metadata object is written out to disk, so it wouldn't be up to date._

`saveTime`: Unix time in seconds _when the photo was saved to disk._ To get the time the photo was taken, use the creature event endpoints.

`width`, `height`: Image size in pixels.

## User Flags

+ 1: Administrator
  A member of a secretive order... or something.

+ 2: Frozen
  Can't login and if already logged in, has most activity shut down.

+ 4: Receive NB Norns
  Receives Norns with species fields that are not 1 or 2. These Norns will crash unprepared clients, so the server blocks them by default.

+ 8: Opt out of NET: RUSO / `any on-line user`
  Some people just don't like random creatures showing up.

+ 16: Receive Geats
  Receives Geats. While patches exist, having two Geats in a world generally leads to the wasteland bug unless the player is very careful. This is obviously not ideal, so don't do it.

+ 32: Muted From Global Chat
  The user is muted from global chat.
