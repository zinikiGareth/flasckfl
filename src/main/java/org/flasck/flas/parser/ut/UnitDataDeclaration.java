package org.flasck.flas.parser.ut;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.ut.UnitTestStep;
import org.flasck.flas.repository.RepositoryEntry;

public class UnitDataDeclaration implements UnitDataFieldConsumer, UnitTestStep, Locatable, RepositoryEntry {
	public static class Assignment {
		public final UnresolvedVar field;
		public final Expr value;

		public Assignment(UnresolvedVar field, Expr value) {
			this.field = field;
			this.value = value;
		}
	}

	private final InputPosition pos;
	private boolean topLevel;
	public final FunctionName name;
	public final TypeReference ofType;
	public final Expr expr;
	public final List<Assignment> fields = new ArrayList<>();

	public UnitDataDeclaration(InputPosition pos, boolean topLevel, TypeReference ofType, FunctionName name, Expr expr) {
		this.pos = pos;
		this.topLevel = topLevel;
		this.ofType = ofType;
		this.name = name;
		this.expr = expr;
	}
	
	@Override
	public NameOfThing name() {
		return name;
	}
	
	public boolean isTopLevel() {
		return topLevel;
	}

	@Override
	public void dumpTo(PrintWriter pw) {
		pw.println(this.toString());
	}

	public InputPosition location() {
		return pos;
	}
	
	@Override
	public void field(UnresolvedVar field, Expr value) {
		fields.add(new Assignment(field, value));
	}

	@Override
	public String toString() {
		return "UnitData[" + name.uniqueName() + "]";
	}
}
