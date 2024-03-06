package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.flas.compiler.jsgen.packaging.JSStorage;
import org.zinutils.bytecode.mock.IndentWriter;

public class InitContext implements IVForm {
	private final JSStorage env;
	private final boolean field;

	public InitContext(JSStorage jse, boolean field) {
		this.env = jse;
		this.field = field;
	}

	@Override
	public void write(IndentWriter w) {
//		String r = "runner";
//		if (field)
//			r = "this._runner";
//		w.println("const _cxt = " + r + ".newContext();");
		if (!field) {
			for (String e : env.packages()) {
				PackageName pp = new PackageName(e);
				if (!e.contains("_ut_") && !e.contains("_st_"))
					w.println("if (" + pp.jsName() + "._init) " + pp.jsName() + "._init(_cxt);");
			}
			w.println("runner.makeReady();");
		}
	}

	@Override
	public void generate(JVMCreationContext jvm) {
//		Var v = jvm.method().avar(J.FLEVALCONTEXT, "_cxt");
//		IExpr r;
//		if (field)
//			r = jvm.method().as(jvm.method().getField("_runner"), FLEvalContextFactory.class.getName());
//		else
//			r = jvm.argAs(new JSVar("runner"), new JavaType(FLEvalContextFactory.class.getName()));
//		IExpr ass = jvm.method().assign(v, jvm.method().callInterface(J.FLEVALCONTEXT, r, "create"));
		jvm.local(this, null);
//		jvm.setCxt(v);
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
