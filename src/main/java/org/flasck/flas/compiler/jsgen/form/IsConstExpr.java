package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.mock.IndentWriter;

public class IsConstExpr implements JSExpr {
	private final JSExpr var;
	private final Integer cnst;
	private final String str;

	public IsConstExpr(JSExpr var, int cnst) {
		this.var = var;
		this.cnst = cnst;
		this.str = null;
	}

	public IsConstExpr(JSExpr var, String str) {
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
			w.print("(" + var.asVar() + " == " + cnst + ")");
		else
			w.print("(" + var.asVar() + " == '" + str + "')");
			
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		NewMethodDefiner meth = jvm.method();
		IExpr myVar = meth.aNull();
		IExpr blk;
		if (cnst != null)
			blk = meth.callInterface(J.BOOLEANP.getActual(), jvm.cxt(), "isConst", myVar, meth.intConst(cnst));
		else
			blk = meth.callInterface(J.BOOLEANP.getActual(), jvm.cxt(), "isConst", myVar, meth.stringConst(str));
		jvm.local(this, blk);
	}

}
