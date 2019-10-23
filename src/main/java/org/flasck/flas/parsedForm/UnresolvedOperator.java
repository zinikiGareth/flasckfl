package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.repository.RepositoryEntry;

public class UnresolvedOperator implements Expr {
	public final InputPosition location;
	public final String op;
	private RepositoryEntry definition;

	public UnresolvedOperator(InputPosition location, String op) {
		this.location = location;
		this.op = op;
	}
	
	@Override
	public InputPosition location() {
		return location;
	}

	@Override
	public String toString() {
		return op;
	}

	public void bind(RepositoryEntry entry) {
		this.definition = entry;
	}
	
	public RepositoryEntry defn() {
		return this.definition;
	}
}
