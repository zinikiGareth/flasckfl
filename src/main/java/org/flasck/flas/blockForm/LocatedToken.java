package org.flasck.flas.blockForm;

public class LocatedToken {
	public final InputPosition location;
	public final String text;
	
	public LocatedToken(InputPosition location, String text) {
		this.location = location;
		this.text = text;
	}
}
