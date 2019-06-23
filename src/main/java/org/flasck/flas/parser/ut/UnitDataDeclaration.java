package org.flasck.flas.parser.ut;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.ut.UnitTestStep;
import org.flasck.flas.repository.RepositoryEntry;

public class UnitDataDeclaration implements RepositoryEntry, UnitDataFieldConsumer, UnitTestStep {
	public static class Assignment {
		public final UnresolvedVar field;
		public final Expr value;

		public Assignment(UnresolvedVar field, Expr value) {
			this.field = field;
			this.value = value;
		}
	}

	public final FunctionName name;
	public final TypeReference ofType;
	public final Expr expr;
	public final List<Assignment> fields = new ArrayList<>();

	public UnitDataDeclaration(TypeReference ofType, FunctionName name, Expr expr) {
		this.ofType = ofType;
		this.name = name;
		this.expr = expr;
	}
	
	@Override
	public void field(UnresolvedVar field, Expr value) {
		fields.add(new Assignment(field, value));
	}

	@Override
	public void dumpTo(PrintWriter pw) {
		pw.println(toString());
	}
	
	@Override
	public String toString() {
		return "UnitData[" + name.uniqueName() + "]";
	}
}