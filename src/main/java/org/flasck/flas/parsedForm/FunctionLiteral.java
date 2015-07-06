package org.flasck.flas.parsedForm;

public class FunctionLiteral {
	public final String name;

	public FunctionLiteral(String text) {
		this.name = text;
	}
	
	@Override
	public String toString() {
		return name + "()";
	}

}
