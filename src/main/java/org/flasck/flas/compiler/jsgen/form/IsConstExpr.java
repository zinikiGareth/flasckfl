package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.zinutils.bytecode.mock.IndentWriter;

public class IsConstExpr implements JSExpr {
	private final String var;
	private final Integer cnst;
	private String str;

	public IsConstExpr(String var, int cnst) {
		this.var = var;
		this.cnst = cnst;
		this.str = null;
	}

	public IsConstExpr(String var, String str) {
		this.var = var;
		this.cnst = null;
		this.str = str;
	}

	@Override
	public String asVar() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void write(IndentWriter w) {
		if (cnst != null)
			w.print("(" + var + " == " + cnst + ")");
		else
			w.print("(" + var + " == '" + str + "')");
			
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		// TODO Auto-generated method stub
		
	}

}
