package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.vcode.hsieForm.HSIEForm.Type;

public class MethodInContext {
	public final Scope scope;
	public final String fromContract;
	public final InputPosition contractLocation;
	public final String name;
	public final Type type;
	public final MethodDefinition method;

	public MethodInContext(Scope scope, InputPosition cloc, String fromContract, String name, Type type, MethodDefinition method) {
		this.scope = scope;
		this.contractLocation = cloc;
		this.fromContract = fromContract;
		this.name = name;
		this.type = type;
		this.method = method;
	}
}
