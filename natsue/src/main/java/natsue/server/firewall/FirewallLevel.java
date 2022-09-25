/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.server.firewall;

/**
 * Firewall settings.
 */
public enum FirewallLevel {
	// minimal: NET: FROM forge protection and that's it.
	// Theoretically secure if you check the NET: FROM of everything all the time.
	// NOT secure with vanilla CAOS.
	minimal,
	// vanillaSafe: Settings that are safe for vanilla Creatures 3/Docking Station. 
	vanillaSafe,
	// full: Block anything that isn't known to be handled by vanilla
	full,
	// rejectAll: FOR TESTING ONLY
	rejectAll
}
