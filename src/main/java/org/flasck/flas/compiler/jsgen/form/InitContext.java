package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.commonBase.names.PackageName;
import org.zinutils.bytecode.mock.IndentWriter;

public class InitContext implements JSExpr {
	private final PackageName packageName;

	public InitContext(PackageName packageName) {
		this.packageName = packageName;
	}

	@Override
	public void write(IndentWriter w) {
		w.println("const _cxt = runner.newContext();");
		w.println(packageName.jsName() + "._init(_cxt);");
	}

	@Override
	public String asVar() {
		throw new RuntimeException("This should be wrapped in a JSLocal or JSThis");
	}
}
