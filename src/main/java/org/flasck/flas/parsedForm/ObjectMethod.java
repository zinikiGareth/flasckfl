package org.flasck.flas.parsedForm;

public class ObjectMethod {
	private final MethodCaseDefn mcd;

	public ObjectMethod(MethodCaseDefn mcd) {
		this.mcd = mcd;
	}

	public MethodCaseDefn getMethod() {
		return mcd;
	}
}
