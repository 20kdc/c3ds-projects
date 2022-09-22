/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package natsue.config;

import natsue.server.database.jdbc.ILDBVariant;

public class ConfigDB extends BaseConfig.Group {
	/**
	 * JDBC connection path.
	 */
	public final Str dbConnection = new Str("dbConnection", "jdbc:sqlite:natsue.db")
			.describe("JDBC connection path. See your JDBC driver documentation for details, in particular in regards to DriverManager.newConnection.");

	/**
	 * Database type
	 */
	public final Emu<ILDBVariant> dbType = new Emu<>("dbType", ILDBVariant.sqlite)
			.describe("Database type. Can be one of: sqlite, mysql");

	/**
	 * Log expected database errors
	 */
	public final Bool logExpectedDBErrors = new Bool("logExpectedDBErrors", false)
			.describe("Log expected database errors - these are expected to occur in normal operation due to, i.e. repeated creature history uploads.");

}
