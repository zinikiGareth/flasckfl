package org.flasck.flas.tc3;

import java.io.PrintWriter;

import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.ObjectCtor;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.TupleAssignment;
import org.flasck.flas.parsedForm.TupleMember;
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
		if (fn.constNess() != null && fn.constNess().isConstant()) {
			pw.print(" [const]");
		}
		pw.println();
	}

	@Override
	public void visitObjectMethod(ObjectMethod om) {
		if (!om.generate)
			return;
		pw.print(om.name().uniqueName());
		pw.print(" :: ");
		if (!om.hasType() || om.type() == null)
			pw.print("<<UNDEFINED>>");
		else
			pw.print(om.type().signature());
		if (om.constNess() != null && om.constNess().isConstant()) {
			pw.print(" [const]");
		}
		pw.println();
	}
	
	@Override
	public void visitObjectCtor(ObjectCtor oc) {
		if (!oc.generate)
			return;
		pw.print(oc.name().uniqueName());
		pw.print(" :: ");
		if (!oc.hasType() || oc.type() == null)
			pw.print("<<UNDEFINED>>");
		else
			pw.print(oc.type().signature());
		if (oc.constNess() != null && oc.constNess().isConstant()) {
			pw.print(" [const]");
		}
		pw.println();
	}

	@Override
	public void visitTuple(TupleAssignment e) {
		// I don't think we actually want to dump this
	}
	
	@Override
	public void visitTupleMember(TupleMember tm) {
		pw.print(tm.name().uniqueName());
		pw.print(" :: ");
		if (tm.type() == null)
			pw.print("<<UNDEFINED>>");
		else
			pw.print(tm.type().signature());
		if (tm.constNess() != null && tm.constNess().isConstant()) {
			pw.print(" [const]");
		}
		pw.println();
	}
}
