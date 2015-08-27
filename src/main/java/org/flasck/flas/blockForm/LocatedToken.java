package org.flasck.flas.blockForm;

import org.flasck.flas.parsedForm.Locatable;

public class LocatedToken implements Comparable<LocatedToken>, Locatable {
	public final InputPosition location;
	public final String text;
	
	public LocatedToken(InputPosition location, String text) {
		this.location = location;
		this.text = text;
	}
	
	@Override
	public InputPosition location() {
		return location;
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
