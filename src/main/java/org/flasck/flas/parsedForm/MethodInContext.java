package org.flasck.flas.parsedForm;

import org.flasck.flas.vcode.hsieForm.HSIEForm.Type;

public class MethodInContext {
	public final Scope scope;
	public final String name;
	public final String suffix;
	public final Type type;
	public final MethodDefinition method;

	public MethodInContext(Scope scope, String name, String suffix, Type type, MethodDefinition method) {
		this.scope = scope;
		this.name = name;
		this.suffix = suffix;
		this.type = type;
		this.method = method;
	}
}
