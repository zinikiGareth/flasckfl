package org.flasck.flas.parsedForm;

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
