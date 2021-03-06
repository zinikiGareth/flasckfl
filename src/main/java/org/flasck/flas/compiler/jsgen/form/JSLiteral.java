package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.mock.IndentWriter;
import org.zinutils.exceptions.NotImplementedException;

public class JSLiteral implements JSExpr {
	private final String text;

	public JSLiteral(String text) {
		this.text = text;
	}

	@Override
	public void write(IndentWriter w) {
		w.print(text);
	}

	@Override
	public String asVar() {
		return text;
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		NewMethodDefiner md = jvm.method();
		IExpr ret = null;
		if ("null".equals(text))
			ret = md.aNull();
		else if ("true".equals(text)) {
			ret = md.boolConst(true);
		} else if ("false".equals(text)) {
			ret = md.boolConst(false);
		} else if ("_card".equals(text) || "this._card".equals(text)) {
			ret = md.getField("_card");
		} else if ("this._card._renderTree".equals(text)) {
			ret = md.getField(md.getField("_card"), "_renderTree");
		} else {
			try {
				double x = Double.parseDouble(text);
				ret = md.makeNew(Double.class.getName(), md.doubleConst(x));
			} catch (NumberFormatException ex) {
				throw new NotImplementedException("non-integer cases: " + text);
			}
		}
		jvm.local(this, ret);
	}

	@Override
	public String toString() {
		return "Literal[" + text + "]";
	}
}
