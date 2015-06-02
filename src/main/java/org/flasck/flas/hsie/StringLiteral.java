package org.flasck.flas.hsie;

public class StringLiteral {
	public final String text;

	public StringLiteral(String text) {
		this.text = text;
	}
	
	@Override
	public String toString() {
		return '"' + text + '"';
	}

}
