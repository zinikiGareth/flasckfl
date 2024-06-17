package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.repository.RepositoryEntry;
import org.zinutils.exceptions.CantHappenException;

public class UnresolvedVar implements Expr {
	public final InputPosition location;
	public final String var;
	private RepositoryEntry definition;

	public UnresolvedVar(InputPosition location, String var) {
		if (var == null)
			throw new CantHappenException("var must be defined");
		this.location = location;
		this.var = var;
	}

	@Override
	public InputPosition location() {
		return location;
	}

	@Override
	public String toString() {
		return var;
	}

	public boolean isCast() {
		return var.equals("cast");
	}
	
	public boolean isType() {
		return var.equals("type");
	}
	
	public boolean isCheckType() {
		return var.equals("istype");
	}
	
	public void bind(RepositoryEntry defn) {
		this.definition = defn;
	}
	
	public RepositoryEntry defn() {
		return definition;
	}
}
