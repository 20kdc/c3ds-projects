/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

/**
 * This code needs a massive rework.
 * It was meant to simulate the game's "many-headed" genome reading 1:1 but it just ended up being a mess.
 * I think when I rewrite this I'll use a more traditional architecture and just try to simulate transcription errors as best as I can.
 */
package cdsp.common.data.genetics;
