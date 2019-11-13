package org.flasck.flas.compiler.jsgen.form;

import org.zinutils.bytecode.mock.IndentWriter;

public class ExtractField implements JSExpr {
	private final String asVar;
	private final String fromVar;
	private final String field;

	public ExtractField(String asVar, String fromVar, String field) {
		this.asVar = asVar;
		this.fromVar = fromVar;
		this.field = field;
	}

	@Override
	public String asVar() {
		return asVar;
	}

	@Override
	public void write(IndentWriter w) {
		w.print("var ");
		w.print(asVar);
		w.print(" = _cxt.field(");
		w.print(fromVar);
		w.print(", '");
		w.print(field);
		w.println("');");
	}

}
