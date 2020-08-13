package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.jvm.J;
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
		} else if ("_card".equals(text)) {
			ret = md.getField("_card");
		} else {
			try {
				int x = Integer.parseInt(text);
				ret = md.makeNew(J.NUMBER, md.box(md.intConst(x)), md.castTo(md.aNull(), "java.lang.Double"));
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
