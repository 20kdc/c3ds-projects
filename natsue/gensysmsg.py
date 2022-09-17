#!/usr/bin/env python3

# c3ds-projects - Assorted compatibility fixes & useful tidbits
# Written starting in 2022 by contributors (see CREDITS.txt)
# To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
# You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

# Generates a system message packet.

import sys
import struct

sv = open(sys.argv[1], "wb")

msghdr = struct.pack("<III", 12, 1, 0)
channel = b"system_message"
content = sys.argv[2].encode("latin1")
msgcontent = struct.pack("<I", len(channel)) + channel + struct.pack("<III", 2469, 2, len(content)) + content + struct.pack("<I", 3)

msgall = msghdr + msgcontent

# use the UID/HID used by natsue0
sender_uid = 3
sender_hid = 4

babelmsg = struct.pack("<IIIIII", 24 + len(msgall), sender_hid, sender_uid, len(msgall), 0, 1) + msgall

packet = struct.pack("<IIIIIIII", 9, 0, 0, 0, 0, 0, len(babelmsg), 0)
packet += babelmsg

sv.write(packet)
sv.close()

