package org.flasck.flas.tc3;

import java.io.PrintWriter;

import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.repository.LeafAdapter;

public class TypeDumper extends LeafAdapter {
	private final PrintWriter pw;

	public TypeDumper(PrintWriter pw) {
		this.pw = pw;
	}

	@Override
	public void visitFunction(FunctionDefinition fn) {
		if (fn.intros().isEmpty())
			return;
		pw.print(fn.name().uniqueName());
		pw.print(" :: ");
		if (fn.type() == null)
			pw.print("<<UNDEFINED>>");
		else
			pw.print(fn.type().signature());
		pw.println();
	}

	@Override
	public void visitObjectMethod(ObjectMethod om) {
		if (om.messages().isEmpty())
			return;
		pw.print(om.name().uniqueName());
		pw.print(" :: ");
		if (!om.hasType() || om.type() == null)
			pw.print("<<UNDEFINED>>");
		else
			pw.print(om.type().signature());
		pw.println();
	}
}
