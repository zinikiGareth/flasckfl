package org.flasck.flas.htmlzip;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Block {

	public class Hole implements Comparable<Hole> {
		private final int from;
		private final int to;
		private final String identity;

		public Hole(int from, int to, String identity) {
			this.from = from;
			this.to = to;
			this.identity = identity;
		}

		@Override
		public int compareTo(Hole o) {
			if (this.from < o.from)
				return -1;
			else if (this.from == o.from)
				return 0;
			else
				return 1;
		}
		
		@Override
		public String toString() {
			return "Hole[" + from + "-" + to + "]";
		}
	}

	public class Attr {
		private final int from;
		private final int to;

		public Attr(int from, int to) {
			this.from = from;
			this.to = to;
		}
	}

	private final String file;
	private final String tag;
	private final int from;
	private final int to;
	private final Set<Hole> holes = new TreeSet<>();
	private final List<Attr> attrs = new ArrayList<>();
	private final Map<String, Hole> ids = new TreeMap<String, Hole>();

	public Block(String file, String tag, int from, int to) {
		this.file = file;
		this.tag = tag;
		this.from = from;
		this.to = to;
	}

	public void addHole(String called, int hs, int ht) {
		if (hs < from || ht > to) {
			System.err.println("Hole from " + hs + " to " + ht + " is not inside " + this);
		} else
			holes.add(new Hole(hs, ht, called));
	}

	public void identify(String called, int is, int it) {
		if (is < from || it > to)
			System.err.println("Cannot identify element with " + called + " because it is outside " + this);
		else {
			ids.put(called, new Hole(is, it, null));
			holes.add(new Hole(is, it, null));
		}
	}

	public void removeAttr(int as, int at, String asId) {
		if (as < from || at > to) {
			System.err.println("Attribute from " + as + " to " + at + " is not inside " + this);
		} else {
			attrs.add(new Attr(as, at));
			holes.add(new Hole(as, at, asId));
		}
	}

	public boolean has(int f, int t) {
		if (f < from || t > to)
			return false;
		for (Hole h : holes) {
			if (h.from < f && h.to > t)
				return false;
		}
		return true;
	}
	
	public void dump() {
		System.out.println("  extracting from " + file);
		int from = this.from;
		for (Hole h : holes) {
			System.out.println("    " + from + "-" + h.from);
			from = h.to;
		}
		System.out.println("    " + from + "-" + this.to);
		
	}
	
	public void generate(PrintWriter pw, File inf) throws IOException {
		String name = "Block_" + tag.replaceAll("-", "_");
		pw.println("function " + name + "(indiv) {");
		ZipInputStream zis = new ZipInputStream(new FileInputStream(inf));
		try {
			ZipEntry ze = null;
			while ((ze = zis.getNextEntry()) != null)
				if (ze.getName().equals(file))
					break;
			if (ze == null)
				throw new RuntimeException("Could not find entry " + file);
			int at = 0;
			int from = this.from;
			pw.println("  this.holes = {};");
			for (Hole h : holes) {
				at = genit(pw, zis, at, from, h.from);
				if (h.identity != null) {
					pw.println("  sb += 'id=\\\'id_';");
					pw.println("  var id = nextId();");
					pw.println("  sb += id;");
					pw.println("  this.holes['" + h.identity + "'] = id;");
					pw.println("  sb += '\\\'';");
				}
				from = h.to;
			}
			genit(pw, zis, at, from, this.to);
			pw.println("  indiv.innerHTML = sb;");
			pw.println("  return sb;");
			pw.println("}");
			pw.println(name + ".prototype.hole = function(name) {");
			pw.println("  return document.getElementById('id_' + this.holes[name]);");
			pw.println("}");
		} finally {
			zis.close();
		}
	}

	private int genit(PrintWriter pw, ZipInputStream zis, int at, int from, final int upto) throws IOException {
		if (at == 0)
			pw.print("  var sb = '");
		else
			pw.print("  sb += '");
		zis.skip(from-at);
		for (at = from;at<upto;at++) {
			char r = (char)zis.read();
			if (r == '\n')
				pw.print("\\n");
			else if (r == '\'')
				pw.print("&apos;");
			else
				pw.print(r);
		}
		pw.println("';");
		return at;
	}
	
	@Override
	public String toString() {
		return "Block [" + tag + " " + from + ":" + to + "]";
	}
}
