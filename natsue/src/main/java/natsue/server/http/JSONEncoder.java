/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package natsue.server.http;

/**
 * This is used for the fancy REST API stuff.
 */
public class JSONEncoder {
	/**
	 * Output buffer.
	 */
	public final StringBuilder out = new StringBuilder();
	/**
	 * If true, a value exists and therefore any further values require a comma.
	 */
	public boolean comma = false;
	public JSONEncoder() {
	}
	// universal value start/end
	private final void startValue() {
		if (comma)
			out.append(',');
	}
	// String/null
	public void write(String text) {
		startValue();
		if (text == null) {
			out.append("null");
		} else {
			out.append('"');
			for (char c : text.toCharArray()) {
				if (c == '"' || c == '\\') {
					out.append('\\');
					out.append(c);
				} else if (c == '\b') {
					out.append("\\b");
				} else if (c == '\f') {
					out.append("\\f");
				} else if (c == '\n') {
					out.append("\\n");
				} else if (c == '\r') {
					out.append("\\r");
				} else if (c == '\t') {
					out.append("\\t");
				} else if (c < 32 || c >= 127) {
					out.append("\\u");
					String ih = Integer.toHexString((int) c);
					while (ih.length() < 4)
						ih = "0" + ih;
					out.append(ih);
				} else {
					out.append(c);
				}
			}
			out.append('"');
		}
		comma = true;
	}
	// Number
	public void write(int number) {
		startValue();
		out.append(number);
		comma = true;
	}
	public void write(long number) {
		startValue();
		// attempt a Frontier
		out.append(number);
		comma = true;
	}
	public void write(float number) {
		startValue();
		out.append(number);
		comma = true;
	}
	public void write(double number) {
		startValue();
		out.append(number);
		comma = true;
	}
	// Object
	public void objectStart() {
		startValue();
		out.append('{');
		comma = false;
	}
	public void objectSplit() {
		if (!comma)
			throw new RuntimeException("objectSplit only makes sense after a value has been written!");
		out.append(':');
		comma = false;
	}
	public void objectEnd() {
		out.append('}');
		comma = true;
	}
	// Array
	public void arrayStart() {
		startValue();
		out.append('[');
		comma = false;
	}
	public void arrayEnd() {
		out.append(']');
		comma = true;
	}
	// Boolean
	public void write(boolean b) {
		startValue();
		out.append(b ? "true" : "false");
		comma = true;
	}
	// Object key proxies
	public void writeKV(String key, String value) { write(key); objectSplit(); write(value); }
	public void writeKV(String key, int value) { write(key); objectSplit(); write(value); }
	public void writeKV(String key, long value) { write(key); objectSplit(); write(value); }
	public void writeKV(String key, float value) { write(key); objectSplit(); write(value); }
	public void writeKV(String key, double value) { write(key); objectSplit(); write(value); }
	public void writeKV(String key, boolean value) { write(key); objectSplit(); write(value); }
}
