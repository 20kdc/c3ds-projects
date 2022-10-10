# Packed Babel Short User Data
You can see one of these at don.dump packet 11, data offset 0x20.


* +0: int len
	* This is the length of this entire structure, including the names.
* +4: B_UIN (distinction from two-int: the upper short is padding)
* +12: int firstNameLen
* +16: int lastNameLen
* +20: int nickNameLen
* +24: The three name components, without separation.
	* char names[firstNameLen + lastNameLen + nickNameLen]


