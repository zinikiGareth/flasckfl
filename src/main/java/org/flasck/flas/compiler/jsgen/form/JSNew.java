package org.flasck.flas.compiler.jsgen.form;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSNew implements JSExpr {
	private final String clz;
	private final List<JSExpr> args;

	public JSNew(NameOfThing clz) {
		this(clz.jsName(), new ArrayList<>());
	}

	public JSNew(String clz) {
		this(clz, new ArrayList<>());
	}
	
	public JSNew(NameOfThing clz, List<JSExpr> args) {
		this(clz.jsName(), args);
	}

	public JSNew(String clz, List<JSExpr> args) {
		this.clz = clz;
		this.args = args;
	}

	@Override
	public String asVar() {
		throw new RuntimeException("This should be wrapped in a JSLocal or JSThis");
	}

	@Override
	public void write(IndentWriter w) {
		w.print("new ");
		w.print(clz);
		w.print("(_cxt");
		for (JSExpr e : args) {
			w.print(", ");
			w.print(e.asVar());
		}
		w.print(")");
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		// TODO Auto-generated method stub
		
	}

}
