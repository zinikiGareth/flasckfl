package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.zinutils.bytecode.mock.IndentWriter;
import org.zinutils.exceptions.NotImplementedException;

public class JSKeepMessages implements JSExpr {
	private final JSExpr ocmsgs;
	private final JSExpr msgs;

	public JSKeepMessages(JSExpr ocmsgs, JSExpr msgs) {
		this.ocmsgs = ocmsgs;
		this.msgs = msgs;
	}

	@Override
	public String asVar() {
		throw new NotImplementedException("Don't call this");
	}

	@Override
	public void write(IndentWriter w, JVMCreationContext jvm) {
		w.println("_cxt.addAll(" + ocmsgs.asVar() + ", " + msgs.asVar() + ");");
	}
}
