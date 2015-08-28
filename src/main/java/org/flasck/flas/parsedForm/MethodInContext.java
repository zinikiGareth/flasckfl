package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.vcode.hsieForm.HSIEForm.Type;

public class MethodInContext {
	public static final int DOWN = 1;
	public static final int UP = 2;
	public static final int EVENT = 3;
	public static final int OBJECT = 4;
	public final Scope scope;
	public final String fromContract;
	public final InputPosition contractLocation;
	public final int direction;
	public final String name;
	public final Type type;
	public final MethodDefinition method;

	public MethodInContext(Scope scope, int dir, InputPosition cloc, String fromContract, String name, Type type, MethodDefinition method) {
		this.scope = scope;
		this.direction = dir;
		this.contractLocation = cloc;
		this.fromContract = fromContract;
		this.name = name;
		this.type = type;
		this.method = method;
	}
}
