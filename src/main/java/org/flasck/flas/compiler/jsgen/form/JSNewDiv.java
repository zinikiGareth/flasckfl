package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.jvm.J;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.mock.IndentWriter;
import org.zinutils.exceptions.NotImplementedException;

public class JSNewDiv implements JSExpr {
	private Integer cnt;

	public JSNewDiv(Integer cnt) {
		this.cnt = cnt;
	}

	@Override
	public String asVar() {
		throw new NotImplementedException("not for use");
	}

	@Override
	public void write(IndentWriter w) {
		w.println("_cxt.newdiv(" + cnt + ")");
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		NewMethodDefiner md = jvm.method();
		jvm.local(this, md.callInterface("void", jvm.argAsIs(new JSVar("_runner")), "newdiv", cnt == null ? md.as(md.aNull(), J.INTEGER) : md.box(md.intConst(cnt))));
	}

}
