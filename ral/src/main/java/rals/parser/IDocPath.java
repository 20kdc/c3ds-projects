/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.parser;

import java.io.IOException;
import java.io.Reader;

/**
 * Handle to a path for relative navigation and so forth.
 * Not to be confused with HoverDoc.
 * BIG IMPORTANT NOTE: equals and hashCode are EXPECTED TO BE IMPLEMENTED.
 */
public interface IDocPath {
	/**
	 * Assuming this path points to a file, opens the document for reading.
	 */
	Reader open() throws IOException;

	/**
	 * Assuming this path points to a file, returns all bytes.
	 */
	byte[] readAllBytes() throws IOException;

	/**
	 * Gets a path relative to this one.
	 * Returning null implies the operation is invalid.
	 */
	IDocPath getRelative(String relPath);

	/**
	 * Returns true if this points to a file.
	 */
	boolean isFile();

	/**
	 * This equates to SrcPosFile.shortName for root files.
	 */
	String getRootShortName();

	/**
	 * Attempts to translate this to an LSP URI.
	 * Returns null if not supported (FileDocPath does this)
	 */
	String toLSPURI();
}
