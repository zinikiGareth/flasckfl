package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.mock.IndentWriter;
import org.zinutils.exceptions.NotImplementedException;

public class JSRequireContract implements JSExpr {
	private final String var;
	private final NameOfThing ctr;

	public JSRequireContract(String var, NameOfThing ctr) {
		this.var = var;
		this.ctr = ctr;
	}

	@Override
	public String asVar() {
		throw new NotImplementedException("cannot ask for the var for this since it is void");
	}

	@Override
	public void write(IndentWriter w) {
		w.print("this._contracts.require(_cxt, '");
		w.print(var);
		w.print("', '");
		w.print(ctr.jsName());
		w.println("');");
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		NewMethodDefiner md = jvm.method();
		IExpr ctrs = md.getField("store");
		IExpr ret = md.callInterface("void", ctrs, "requireService", jvm.cxt(),
				md.classConst(ctr.javaName()), md.stringConst(var));
		jvm.local(this, ret);
	}

}
