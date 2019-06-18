package org.flasck.flas.parser.ut;

import java.io.PrintWriter;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.repository.RepositoryEntry;

public class UnitDataDeclaration implements RepositoryEntry, UnitDataFieldConsumer {
	public final FunctionName name;
	public final TypeReference ofType;
	public final Expr expr;

	public UnitDataDeclaration(TypeReference ofType, FunctionName name, Expr expr) {
		this.ofType = ofType;
		this.name = name;
		this.expr = expr;
	}
	
	@Override
	public void field(UnresolvedVar field, Expr value) {
		throw new org.zinutils.exceptions.NotImplementedException();
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
