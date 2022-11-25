/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

/**
 * The only one that should ever normally be used outside of the LSP.
 */
public class FileDocPath implements IDocPath {
	public final File file;

	public FileDocPath(File f) {
		File r = f.getAbsoluteFile();
		try {
			// This might be a problem b/c of symlinks.
			// But it deals with the ".." problem.
			r = r.getCanonicalFile();
		} catch (IOException ioe) {
			// oh no you don't
		}
		file = r;
	}

	@Override
	public int hashCode() {
		return file.hashCode();
	}

	@Override
	public boolean equals(Object var1) {
		if (var1 instanceof FileDocPath)
			return ((FileDocPath) var1).file.equals(file);
		return false;
	}

	@Override
	public Reader open() throws IOException {
		return new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
	}

	@Override
	public IDocPath getRelative(String relPath) {
		return new FileDocPath(new File(file, relPath));
	}

	@Override
	public boolean isFile() {
		return file.isFile();
	}

	@Override
	public String getRootShortName() {
		return file.getName();
	}

	@Override
	public String toString() {
		return file.toString();
	}

	@Override
	public String toLSPURI() {
		return null;
	}
}
