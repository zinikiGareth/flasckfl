package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSSplitRWM implements JSExpr {
	private final JSExpr ocmsgs;
	private final String var;

	public JSSplitRWM(JSExpr ocmsgs, String var) {
		this.ocmsgs = ocmsgs;
		this.var = var;
	}

	@Override
	public String asVar() {
		return var;
	}

	@Override
	public void write(IndentWriter w) {
		w.println("if (" + var + " instanceof ResponseWithMessages) {");
		IndentWriter iw = w.indent();
		iw.println("_cxt.addAll(" + ocmsgs.asVar() + ", ResponseWithMessages.messages(_cxt, " + var + "));");
		iw.println(var + " = ResponseWithMessages.response(_cxt, " + var + ");");
		w.println("}");
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		// TODO Auto-generated method stub
		
	}
}
