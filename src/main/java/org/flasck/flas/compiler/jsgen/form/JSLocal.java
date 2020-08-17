package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JSMethod;
import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.Var;
import org.zinutils.bytecode.mock.IndentWriter;
import org.zinutils.exceptions.NotImplementedException;

public class JSLocal implements IVForm {
	private final JSMethod meth;
	private final JSExpr value;
	private String var;

	public JSLocal(JSMethod meth, JSExpr value) {
		this.meth = meth;
		this.value = value;
	}

	public JSLocal(JSMethod meth, JSVar var, JSExpr value) {
		this.meth = meth;
		this.var = var.asVar();
		this.value = value;
	}

	@Override
	public String asVar() {
		if (var == null)
			var = meth.obtainNextVar();
		return var;
	}

	@Override
	public void write(IndentWriter w) {
		if (var == null)
			var = meth.obtainNextVar();
		w.print("const ");
		w.print(var);
		w.print(" = ");
		value.write(w);
		w.println(";");
	}
	
	@Override
	public void generate(JVMCreationContext jvm) {
		IExpr arg = jvm.argAsIs(value);
		if (arg == null)
			throw new NotImplementedException("there is no value for " + value.getClass() + " " + value);
		NewMethodDefiner md = jvm.method();
		Var v;
		if (arg.getType().equals("boolean"))
			v = md.ivar("boolean", var);
		else
			v = md.avar(arg.getType(), var);
		jvm.local(this, md.assign(v, arg));
		jvm.bindVar(this, v);
	}

	@Override
	public void asivm(IVFWriter iw) {
		iw.print(asVar() + " <- ");
		iw.write(value);
		iw.println("");
	}
}
