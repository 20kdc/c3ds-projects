/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package rals.tooling;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;

import rals.parser.FileDocPath;
import rals.parser.IDocPath;

/**
 * Contains reference copies of documents for use in editing.
 */
public class LSPDocRepo {
	private static final String lspURLEncoding = "UTF-8";

	/**
	 * This index specifically uses canonical file paths in URIs.
	 */
	private HashMap<IDocPath, String> stored = new HashMap<>();

	/**
	 * This index is of Files that the LSP client has sent URIs for.
	 * It deliberately leaks, this is in case the LSP client starts mucking around...
	 */
	private HashMap<File, String> uriLSPClientCanon = new HashMap<>();

	private File decodeFileURI(String uri) {
		if (uri.startsWith("file://"))
			try {
				return new File(URLDecoder.decode(uri.substring(7), lspURLEncoding)).getAbsoluteFile();
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
		return null;
	}

	private String encodeFileURI(File file) {
		String ovr = uriLSPClientCanon.get(file);
		if (ovr != null)
			return ovr;
		try {
			return "file://" + URLEncoder.encode(file.getAbsolutePath(), lspURLEncoding);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Alters the shadow file repository given the ICanonURIDocPath.
	 */
	public void storeShadow(IDocPath path, String text) {
		if (text == null) {
			stored.remove(path);
		} else {
			stored.put(path, text);
		}
	}

	public IDocPath getDocPath(String uri) {
		final File decodedAsFile = decodeFileURI(uri);
		if (decodedAsFile != null) {
			uriLSPClientCanon.put(decodedAsFile, uri);
			return new ShadowableFDP(decodedAsFile);
		}
		return new ExternalDP(uri);
	}

	public IDocPath getDocPath(File f) {
		return new ShadowableFDP(f);
	}

	/**
	 * This is a file that might be shadowed by the LSP client.
	 */
	public class ShadowableFDP extends FileDocPath {
		public ShadowableFDP(File f) {
			// this canonizes it...
			super(f);
		}

		@Override
		public Reader open() throws IOException {
			String override = stored.get(this);
			if (override != null)
				return new StringReader(override);
			return super.open();
		}

		@Override
		public boolean isFile() {
			if (stored.containsKey(this))
				return true;
			return super.isFile();
		}

		@Override
		public IDocPath getRelative(String relPath) {
			return new ShadowableFDP(new File(file, relPath));
		}

		@Override
		public String toString() {
			String base = super.toString();
			return stored.containsKey(this) ? ("*" + base) : base;
		}

		@Override
		public String getRootShortName() {
			String base = super.getRootShortName();
			return stored.containsKey(this) ? ("*" + base) : base;
		}

		@Override
		public String toLSPURI() {
			return encodeFileURI(file);
		}
	}

	/**
	 * This isn't a file:// URI, so it entirely lives in the LSP client.
	 */
	public class ExternalDP implements IDocPath {
		public final String uri;

		public ExternalDP(String u) {
			uri = u;
		}

		@Override
		public int hashCode() {
			return uri.hashCode();
		}

		@Override
		public boolean equals(Object var1) {
			if (var1 instanceof ExternalDP)
				return ((ExternalDP) var1).uri.equals(var1);
			return false;
		}

		@Override
		public Reader open() throws IOException {
			String override = stored.get(this);
			if (override != null)
				return new StringReader(override);
			throw new FileNotFoundException("URI " + uri + " not available");
		}

		@Override
		public IDocPath getRelative(String relPath) {
			return null;
		}

		@Override
		public boolean isFile() {
			return stored.containsKey(this);
		}

		@Override
		public String getRootShortName() {
			return uri;
		}

		@Override
		public String toString() {
			return uri;
		}

		@Override
		public String toLSPURI() {
			return uri;
		}
	}
}
