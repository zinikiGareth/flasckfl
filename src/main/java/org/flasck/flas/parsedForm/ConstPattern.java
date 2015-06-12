package org.flasck.flas.parsedForm;

public class ConstPattern {
	public static final int INTEGER = 1;
	public static final int BOOLEAN = 2;

	public final int type;
	public final String value;

	public ConstPattern(int type, String value) {
		this.type = type;
		this.value = value;
	}
	
	@Override
	public String toString() {
		return value;
	}
}
