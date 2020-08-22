package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.commonBase.names.CSName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.mock.IndentWriter;
import org.zinutils.exceptions.NotImplementedException;

public class JSRecordContract implements JSExpr {
	private final NameOfThing ctr;
	private final CSName impl;

	public JSRecordContract(NameOfThing ctr, CSName impl) {
		this.ctr = ctr;
		this.impl = impl;
	}

	@Override
	public String asVar() {
		throw new NotImplementedException("cannot ask for the var for this since it is void");
	}

	@Override
	public void write(IndentWriter w) {
		w.print("this._contracts.record(_cxt, '");
		w.print(ctr.jsName());
		w.print("', new ");
		w.print(impl.jsName());
		w.println("(_cxt, this));");
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		NewMethodDefiner md = jvm.method();
		IExpr ctrs = md.getField("store");
		IExpr ret = md.callInterface("void", ctrs, "recordContract",
				md.stringConst(ctr.uniqueName()),
				md.as(md.makeNew(impl.javaName(), md.getArgument(0),
						md.as(md.myThis(), J.OBJECT)), J.OBJECT));
		jvm.local(this, ret);
	}

}
