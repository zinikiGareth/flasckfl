package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.zinutils.bytecode.mock.IndentWriter;
import org.zinutils.exceptions.NotImplementedException;

public class JSRecordContract implements JSExpr {

	private final String ctr;
	private final String impl;

	public JSRecordContract(String ctr, String impl) {
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
		w.print(ctr);
		w.print("', new ");
		w.print(impl);
		w.println("(_cxt, this));");
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		// TODO Auto-generated method stub
		
	}

}
