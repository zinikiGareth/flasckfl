package org.flasck.flas.compiler.jsgen.form;

import java.io.PrintWriter;

import org.zinutils.bytecode.mock.IndentWriter;

public class IVFWriter extends IndentWriter {

	public IVFWriter(PrintWriter pw) {
		super(pw);
	}

	public IVFWriter(PrintWriter pw, String ind) {
		super(pw, ind);
	}

	public void write(JSExpr e) {
		if (e instanceof IVForm)
			((IVForm)e).asivm(this);
		else
			print(e.toString());
	}
	
	@Override
	public IVFWriter indent() {
		return new IVFWriter(pw, ind+"  ");
	}
}
