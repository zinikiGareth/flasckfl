package org.flasck.flas.tc3;

import java.io.PrintWriter;

import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.repository.LeafAdapter;

public class TypeDumper extends LeafAdapter {
	private final PrintWriter pw;

	public TypeDumper(PrintWriter pw) {
		this.pw = pw;
	}

	@Override
	public void visitFunction(FunctionDefinition fn) {
		pw.print(fn.name().uniqueName());
		pw.print(" :: ");
		pw.print(fn.type().signature());
		pw.println();
	}
}
