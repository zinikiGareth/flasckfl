package org.flasck.flas.htmlzip;

import java.util.Set;
import java.util.TreeSet;

public class Block {

	public class Hole implements Comparable<Hole> {
		private final int from;
		private final int to;
		private final String holeName;
		private final String idAttr;

		public Hole(int from, int to, String holeName, String idAttr) {
			this.from = from;
			this.to = to;
			this.holeName = holeName;
			this.idAttr = idAttr;
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

	private final String file;
	private final String tag;
	private final int from;
	private final int to;
	private final Set<Hole> holes = new TreeSet<>();

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
			holes.add(new Hole(hs, ht, called, null));
	}

	public void identityAttr(String called, int is, int it) {
		if (is < from || it > to)
			System.err.println("Cannot identify element with " + called + " because it is outside " + this);
		else {
			holes.add(new Hole(is, it, null, called));
		}
	}

	public void removeAttr(int as, int at) {
		if (as < from || at > to) {
			System.err.println("Attribute from " + as + " to " + at + " is not inside " + this);
		} else {
			holes.add(new Hole(as, at, null, null));
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
			if (h.from > from)
				visitor.render(from, h.from);
			if (h.holeName != null)
				visitor.renderIntoHole(h.holeName);
			else if (h.idAttr != null)
				visitor.id(h.idAttr);
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
			public void renderIntoHole(String holeName) {
				System.out.println("    hole " + holeName);
			}

			@Override
			public void id(String id) {
				System.out.println("    id attr " + id);
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
