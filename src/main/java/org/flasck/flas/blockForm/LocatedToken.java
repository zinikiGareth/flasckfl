package org.flasck.flas.blockForm;


public class LocatedToken implements Comparable<LocatedToken> {
	public final InputPosition location;
	public final String text;
	
	public LocatedToken(InputPosition location, String text) {
		this.location = location;
		this.text = text;
	}

	@Override
	public int compareTo(LocatedToken other) {
		return this.text.compareTo(other.text);
	}
	
	@Override
	public String toString() {
		return "Tok[" + text + "@"+location +"]";
	}
}
