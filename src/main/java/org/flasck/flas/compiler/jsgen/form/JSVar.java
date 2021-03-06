package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.jvm.J;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSVar implements JSExpr {
	private final String type;
	private final String name;

	public JSVar(String name) {
		this.type = J.OBJECT;
		this.name = name;
	}
	
	public JSVar(String type, String name) {
		this.type = type;
		this.name = name;
	}

	@Override
	public void write(IndentWriter w) {
		w.print(name);
	}

	public String type() {
		return type;
	}
	
	@Override
	public String asVar() {
		return name;
	}

	@Override
	public void generate(JVMCreationContext jvm) {
	}
	
	@Override
	public boolean equals(Object obj) {
		return obj instanceof JSVar && ((JSVar)obj).name.equals(name);
	}
	
	@Override
	public int hashCode() {
		return name.hashCode();
	}
	
	@Override
	public String toString() {
		return "JSVar[" + name + "]";
	}
}
