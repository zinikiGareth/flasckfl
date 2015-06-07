package org.flasck.flas.parsedForm;

public class NumericLiteral {
	public final String text;

	public NumericLiteral(String text) {
		this.text = text;
	}

	@Override
	public String toString() {
		return text;
	}
}
