package org.flasck.flas.parsedForm.ut;

import java.io.PrintWriter;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.UnitTestName;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parser.ut.UnitTestStepConsumer;
import org.flasck.flas.repository.RepositoryEntry;

public class UnitTestCase implements UnitTestStepConsumer, RepositoryEntry {
	public final UnitTestName name;
	public final String description;

	public UnitTestCase(UnitTestName name, String description) {
		this.name = name;
		this.description = description;
	}

	@Override
	public void assertion(Expr expr, Expr value) {
	}

	@Override
	public void event(UnresolvedVar card, StringLiteral name, Expr event) {
	}

	@Override
	public void send(UnresolvedVar card, TypeReference contract, Expr invocation) {
	}

	@Override
	public void template() {
	}

	@Override
	public void dumpTo(PrintWriter pw) {
		pw.println(toString());
	}
	
	@Override
	public String toString() {
		return "UnitTestCase[" + description + "]";
	}
}
