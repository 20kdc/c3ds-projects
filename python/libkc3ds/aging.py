#!/usr/bin/env python3
# Database of templates.

# libkc3ds - General-purpose Python library for C3/DS
# Written starting in 2023 by contributors (see CREDITS.txt)
# To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
# You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

from ._aging_dsc import CHICHI
from ._aging_c3g import C3_GRENDEL
from ._aging_c3e import C3_ETTIN
from ._aging_defs import CSet, CAge, AgedPart

CSETS_ALL = [CHICHI, C3_GRENDEL, C3_ETTIN]

