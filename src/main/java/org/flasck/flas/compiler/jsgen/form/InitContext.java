package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.flas.compiler.jsgen.packaging.JSStorage;
import org.zinutils.bytecode.mock.IndentWriter;

public class InitContext implements JSExpr {
	private final JSStorage env;

	public InitContext(PackageName packageName, JSStorage jse) {
		this.env = jse;
	}

	@Override
	public void write(IndentWriter w) {
		w.println("const _cxt = runner.newContext();");
		for (String e : env.packages())
			if (!e.contains("_ut_"))
				w.println(e + "._init(_cxt);");
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		// I don't think this is needed in JVM land
		jvm.local(this, null);
	}
	
	@Override
	public String asVar() {
		throw new RuntimeException("This should be wrapped in a JSLocal or JSThis");
	}
}
