# Hosting Your Own Natsue Instance

Before you continue with this guide, have you considered using the `eemfoo.org` instance of Natsue?

...no, if you're reading this at all you probably don't want to.

## Disclaimers

1. Natsue does not have any form of federation mechanism. The trust assumptions made by the Babel protocol are hard enough to secure by themselves.
2. Natsue does not operate on a strict release policy. You're expected to use the `master` branch.
3. It is not safe to change the server (or rather, user database) of a world after that world first logs into a server.
4. Running multiple servers on the same database is *not a supported usecase.*

## Requirements

* Something vaguely shaped like, or at least emulating, a computer that can run the below software.
* Something that can run Docking Station (this is listed as a requirement *solely for testing and issuing administrative commands* and it need not be active at all times)
* A Java JDK with a version above or equal to 8.
	* If you somehow don't have a system that packages one of these, maybe look at https://adoptium.net/en-gb/temurin/releases/ ?
* At least one of:
	* Something to extract `.zip` files
	* Something to extract `.tar.gz` files
	* Git
	* Some other unforeseen way to acquire the contents of this repository

## Procedure

1. Ensure the above requirements are *installed.* Java must be available from your terminal/command prompt. Try `java --version` if you're unsure.
2. Somehow acquire the Natsue source code, say, by downloading this repository using Git or as a `.zip` file or a `.tar.gz` file.
3. Using a terminal (or Command Prompt on Windows), `cd` to the `c3ds-projects/natsue` directory. (You may need to use a backslash on Windows.)
4. Assuming this succeeds, then run `build`.
5. Assuming this succeeds, the file `natsue-server-cradle-0.666-SNAPSHOT-jar-with-dependencies.jar` now exists in the `c3ds-projects/natsue/cradle/target` directory. While still within the `c3ds-projects/natsue/cradle` directory, run `java -jar target/natsue-server-cradle-0.666-SNAPSHOT-jar-with-dependencies.jar` to start the server for the first time.
6. Assuming *that* succeeds, immediately stop the server (Control-C on Linux, who knows on Windows).
7. Modify the generated `ntsuconf.txt` file to your liking. The meaning of each option is listed within the file.
8. Restart the server with the same `java -jar target/natsue-server-cradle-0.666-SNAPSHOT-jar-with-dependencies.jar` command.
9. Copy the provided `server.cfg` to your Docking Station directory and modify it to fit your server's IP and port. Repeat for all installations you wish to connect to the server.

## Administrative Commands

`!System` contains a number of administrative commands, for administrators. Ask it for more details.

*In order to grant yourself administrative access, you will need to manually edit the database. Within the `natsue_users` table, find the entry for you and change your `flags` from `0` to `1`.*

Once you have admin access you can grant it to others using the `flags` command. To manually edit the database, use a tool such as https://sqlitebrowser.org/ or whatever is applicable to your server.

For a list of all flags appliable to accounts, check the table in [Web API](WebAPI.md).

## How To Parse A Full Report

The command `fullreportfile`, for administrators, creates a "full report file". This file is saved into your `Warp In` directory (in `Users/yourusername`) using a chunk that is known to be ignored by vanilla Docking Station.

This file is, quite simply, a text file written using the chunk ID `INVI`. A PRAY decompiler *should* get it out. The actual contents are subject to change, as it's meant for debugging.

## The Web UI

The web UI exists so that Natsue can operate as an "all-in-one" solution, covering things like online creature history without requiring a separate service to implement it.

Using the [Web API](WebAPI.md), you can write your own replacement UI.

## Web UI Customization

If a file called `kisspopui.html` exists in the directory, the server will template web UI pages using this file.

Three strings are replaced with various content:

* `$PAGE_TITLE`: Replaced with the page title.
* `$SHIT_GOES_HERE`: Replaced with the usual HTML.
* `$NATSUE_VERSION`: Replaced with the Natsue version link.
