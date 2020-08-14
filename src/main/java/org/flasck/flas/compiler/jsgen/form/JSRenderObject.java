package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSRenderObject implements IVForm {
	private final JSExpr runner;
	private final JSExpr obj;
	private final SolidName on;
	private final int which;
	private final JSString templateName;

	public JSRenderObject(JSExpr runner, JSExpr obj, SolidName on, int which, JSString templateName) {
		this.runner = runner;
		this.obj = obj;
		this.on = on;
		this.which = which;
		this.templateName = templateName;
	}

	public void write(IndentWriter w) {
		runner.write(w);
		w.print(".");
		w.print("render");
		w.print("(_cxt");
		w.print(", ");
		w.print(new JSNameOf(obj).asVar());
		w.print(", ");
		w.print(on.jsName()+".prototype._updateTemplate" + which);
		w.print(", ");
		w.print(templateName.asVar());
		w.println(");");
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		NewMethodDefiner md = jvm.method();
		IExpr ret = md.callInterface("void", jvm.argAsIs(runner), "render", jvm.cxt(), jvm.arg(obj), md.intConst(which), md.stringConst(templateName.value()));
		jvm.local(this, ret);
	}

	@Override
	public void asivm(IVFWriter iw) {
		iw.println("render");
	}
	
	@Override
	public String asVar() {
		// TODO Auto-generated method stub
		return null;
	}
}
