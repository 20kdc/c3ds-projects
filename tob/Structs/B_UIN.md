# B UIN
Identifies a specific user.
Represented by the form ``0+1`` where 0 is the UID and 1 is the HID.

To verify this, check NetUtilities::UserToString.

*These IDs are persistent - the description of NET: USER makes this clear enough if nothing else.*

**Be wary!** *Not all UID/HID pairs are B_UINs. The difference is that the padding bytes may be uninitialized and are invalid.*
*Also never have a negative UID or HID - the filename format used for [InboxOutbox](../Concepts/InboxOutbox.md) won't appreciate it.*

Structure
---------

	+0: UID
	    int UID
	+4: HID
	    short HID
	+6: 2 bytes of padding?
	    short padding

