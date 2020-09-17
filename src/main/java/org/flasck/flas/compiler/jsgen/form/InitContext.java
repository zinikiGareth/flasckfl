package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.flas.compiler.jsgen.packaging.JSStorage;
import org.flasck.jvm.J;
import org.flasck.jvm.container.FLEvalContextFactory;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.JavaType;
import org.zinutils.bytecode.Var;
import org.zinutils.bytecode.mock.IndentWriter;

public class InitContext implements IVForm {
	private final JSStorage env;

	public InitContext(JSStorage jse) {
		this.env = jse;
	}

	@Override
	public void write(IndentWriter w) {
		w.println("const _cxt = runner.newContext();");
		for (String e : env.packages())
			if (!e.contains("_ut_") && !e.contains("_st_"))
				w.println(e + "._init(_cxt);");
		w.println("runner.makeReady();");
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		Var v = jvm.method().avar(J.FLEVALCONTEXT, "_cxt");
		IExpr ass = jvm.method().assign(v, jvm.method().callInterface(J.FLEVALCONTEXT, jvm.argAs(new JSVar("runner"), new JavaType(FLEvalContextFactory.class.getName())), "create"));
		jvm.bindVar(this, v);
		jvm.local(this, ass);
	}
	
	@Override
	public String asVar() {
		throw new RuntimeException("This should be wrapped in a JSLocal or JSThis");
	}

	@Override
	public void asivm(IVFWriter iw) {
		iw.println("create context");
	}
}
