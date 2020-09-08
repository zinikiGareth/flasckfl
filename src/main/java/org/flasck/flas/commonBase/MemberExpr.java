package org.flasck.flas.commonBase;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.tc3.Type;
import org.zinutils.exceptions.NotImplementedException;
import org.zinutils.exceptions.UtilException;

public class MemberExpr implements Expr {
	public final InputPosition location;
	public final Expr from;
	public final Expr fld;
	private RepositoryEntry entry;
	private Expr conversion;
	private ContractMethodDecl contractMethod;
	private Type containerType;
	private Type containedType;
	private boolean boundEarly;

	public MemberExpr(InputPosition location, Expr from, Expr fld) {
		if (location == null)
			throw new UtilException("MemberExpr without location");
		this.location = location;
		this.from = from;
		this.fld = fld;
	}

	@Override
	public InputPosition location() {
		return location;
	}

	public String asName() {
		if (from instanceof UnresolvedVar)
			return ((UnresolvedVar)from).var + "." + fld;
		else if (from instanceof MemberExpr) {
			String n = ((MemberExpr)from).asName();
			if (n == null)
				return null;
			else
				return n + "." + fld;
		} else
			return null;
	}
	
	public void bindContainerType(Type ty) {
		this.containerType = ty;
	}
	
	public Type containerType() {
		return this.containerType;
	}
	
	public void bindContainedType(Type ty) {
		this.containedType = ty;
	}
	
	public Type containedType() {
		return this.containedType;
	}
	
	public void showTree(int ind) {
		showOne(ind, from);
		showOne(ind, fld);
	}

	private void showOne(int ind, Object o) {
		for (int i=0;i<ind;i++)
			System.out.print(" ");
		System.out.println(o);
	}
	
	@Override
	public String toString() {
		if (boundEarly) {
			return entry.name().uniqueName();
		}
		StringBuilder ret = new StringBuilder();
		ret.append("(. ");
		ret.append(" ");
		ret.append(from);
		ret.append(" ");
		ret.append(fld);
		ret.append(")");
		return ret.toString();
	}

	public void conversion(Expr expr) {
		this.conversion = expr;		
	}

	public boolean isConverted() {
		return this.conversion != null;
	}

	public Expr converted() {
		if (conversion == null)
			throw new NotImplementedException("there is no converted expression");
		return conversion;
	}

	public void bindContractMethod(ContractMethodDecl method) {
		this.contractMethod = method;
	}
	
	public ContractMethodDecl contractMethod() {
		return contractMethod;
	}

	public void bind(RepositoryEntry entry, boolean early) {
		this.entry = entry;
		this.boundEarly = early;
	}
	
	public boolean boundEarly() {
		return boundEarly;
	}
	
	public RepositoryEntry defn() {
		return entry;
	}
}
