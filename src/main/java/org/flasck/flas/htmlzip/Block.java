package org.flasck.flas.htmlzip;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
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

	public class RemovedAttr {
		private final int from;
		private final int to;

		public RemovedAttr(int from, int to) {
			this.from = from;
			this.to = to;
		}
	}

	private final String file;
	private final String tag;
	private final int from;
	private final int to;
	private final Set<Hole> holes = new TreeSet<>();
	private final List<RemovedAttr> removedAttrs = new ArrayList<>();
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

	public void removeAttr(int as, int at) {
		if (as < from || at > to) {
			System.err.println("Attribute from " + as + " to " + at + " is not inside " + this);
		} else {
			removedAttrs.add(new RemovedAttr(as, at));
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

	public void visit(CardVisitor visitor) {
		visitor.consider(file);
		int from = this.from;
		for (Hole h : holes) {
			visitor.render(from, h.from);
			from = h.to;
		}
		visitor.render(from, this.to);
		visitor.done();
	}
	
	public void dump() {
		visit(new CardVisitor() {
			@Override
			public void consider(String file) {
				System.out.println("  extracting from " + file);
			}
			
			@Override
			public void render(int from, int to) {
				System.out.println("    " + from + "-" + to);
			}
			
			@Override
			public void done() {
			}
		});
	}
	
	@Override
	public String toString() {
		return "Block [" + tag + " " + from + ":" + to + "]";
	}
}
