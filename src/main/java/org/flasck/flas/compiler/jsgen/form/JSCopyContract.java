package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.zinutils.bytecode.mock.IndentWriter;
import org.zinutils.exceptions.NotImplementedException;

public class JSCopyContract implements JSExpr {

	private final JSExpr copyInto;
	private final String fld;
	private final String arg;

	public JSCopyContract(JSExpr copyInto, String fld, String arg) {
		this.copyInto = copyInto;
		this.fld = fld;
		this.arg = arg;
	}

	@Override
	public String asVar() {
		throw new NotImplementedException();
	}

	@Override
	public void write(IndentWriter w, JVMCreationContext jvm) {
		w.print(copyInto.asVar());
		w.print(".");
		w.print(fld);
		w.print(" = ");
		w.print(arg);
		w.println(";");
	}

}
