package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSString implements JSExpr {
	private final String text;

	public JSString(String text) {
		this.text = text;
	}

	@Override
	public void write(IndentWriter w) {
		w.print(asVar());
	}

	@Override
	public String asVar() {
		StringBuilder ret = new StringBuilder(text);
		int idx = -1;
		for(;;) {
			idx = ret.indexOf("'", idx+1);
			if (idx != -1) {
				ret.insert(idx, "\\");
				idx += 2;
			} else
				break;
		}
		ret.insert(0, "'");
		ret.append("'");
		return ret.toString();
	}

	public String value() {
		return text;
	}

	@Override
	public void generate(JVMCreationContext jvm) {
	}
	
	@Override
	public String toString() {
		return asVar();
	}
}
