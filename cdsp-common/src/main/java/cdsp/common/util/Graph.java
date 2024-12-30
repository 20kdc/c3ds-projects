/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package cdsp.common.util;

import java.util.HashSet;

/**
 * Graph. Used for analysis of reaction chains.
 */
public class Graph {
	public final boolean directed;
	public HashSet<Node> nodes = new HashSet<>();
	public HashSet<Link> links = new HashSet<>();

	public Graph(boolean directed) {
		this.directed = directed;
	}

	public void export(StringBuilder sb) {
		if (directed) {
			sb.append("digraph gr {\n");
		} else {
			sb.append("graph gr {\n");
		}
		for (Node node : nodes) {
			sb.append(" ");
			exportString(sb, node.id);
			sb.append(" [label=");
			exportString(sb, node.text);
			sb.append("];\n");
		}
		for (Link link : links) {
			sb.append(" ");
			exportString(sb, link.from.id);
			if (directed) {
				sb.append(" -> ");
			} else {
				sb.append(" -- ");
			}
			exportString(sb, link.to.id);
			if (link.text != null) {
				sb.append(" [label=");
				exportString(sb, link.text);
				sb.append("]");
			}
			sb.append(";\n");
		}
		sb.append("}\n");
	}

	private void exportString(StringBuilder sb, String id) {
		sb.append('\"');
		sb.append(id);
		sb.append('\"');
	}

	public static final class Node {
		/**
		 * Node ID.
		 */
		public final String id;
		/**
		 * Node text.
		 */
		public String text;

		public Node(String id) {
			this.id = id;
			text = id;
		}

		/**
		 * Set text.
		 */
		public Node withText(String text) {
			this.text = text;
			return this;
		}
	}

	public static final class Link {
		/**
		 * From/To nodes.
		 */
		public final Node from, to;

		/**
		 * Link text.
		 */
		public String text = null;

		public Link(Node f, Node t) {
			from = f;
			to = t;
		}

		/**
		 * Set text.
		 */
		public Link withText(String text) {
			this.text = text;
			return this;
		}
	}
}
