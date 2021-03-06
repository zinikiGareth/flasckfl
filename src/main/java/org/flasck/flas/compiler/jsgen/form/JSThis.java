package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.zinutils.bytecode.mock.IndentWriter;
import org.zinutils.exceptions.NotImplementedException;

public class JSThis implements JSExpr {

	@Override
	public String asVar() {
		return "this";
	}

	@Override
	public void write(IndentWriter w) {
		throw new NotImplementedException();
	}
	
	@Override
	public void generate(JVMCreationContext jvm) {
		jvm.local(this, jvm.method().myThis());
	}
	
	@Override
	public int hashCode() {
		return "this".hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		return obj instanceof JSThis;
	}

	@Override
	public String toString() {
		return "this";
	}
}
