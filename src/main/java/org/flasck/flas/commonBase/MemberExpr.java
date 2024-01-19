package org.flasck.flas.commonBase;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.FieldAccessor;
import org.flasck.flas.parsedForm.ObjectActionHandler;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.StateHolder;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.UnionTypeDefn;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.assembly.ApplicationRouting;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.tc3.ErrorType;
import org.flasck.flas.tc3.PolyInstance;
import org.flasck.flas.tc3.Primitive;
import org.flasck.flas.tc3.Type;
import org.flasck.flas.tc3.TypeConstraintSet;
import org.zinutils.exceptions.HaventConsideredThisException;
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
		else if (from instanceof TypeReference) {
			return ((TypeReference)from).name() + "." + fld;
		}
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
		if (ty instanceof Primitive || ty instanceof UnionTypeDefn || ty instanceof ErrorType)
			return;
		if (this.fld instanceof UnresolvedVar) {
			UnresolvedVar uv = (UnresolvedVar) this.fld;
			RepositoryEntry e = null;
			if (uv.defn() == null) {
				if (ty instanceof TypeConstraintSet) {
					TypeConstraintSet tcs = (TypeConstraintSet)ty;
					if (tcs.isResolved())
						ty = tcs.resolvedTo();
					else
						return;
				}
				if (ty instanceof PolyInstance) {
					ty = ((PolyInstance)ty).struct();
				}
				if (ty instanceof Primitive || ty instanceof UnionTypeDefn)
					return;
				if (ty instanceof StructDefn) {
					StructDefn sd = (StructDefn) ty;
					e = sd.findField(uv.var);
				} else if (ty instanceof StateHolder) {
					StateHolder cd = (StateHolder) ty;
					if (cd.state() != null) // it may be a field
						e = cd.state().findField(uv.var);
					if (e == null) {
						if (ty instanceof ObjectDefn) {
							ObjectDefn od = (ObjectDefn) ty;
							ObjectActionHandler ctor = od.getConstructor(uv.var);
							if (ctor != null)
								e = ctor;
							if (e == null) {
								FieldAccessor acor = od.getAccessor(uv.var);
								if (acor != null)
									e = (RepositoryEntry) acor;
							}
							if (e == null) {
								ObjectMethod method = od.getMethod(uv.var);
								if (method != null)
									e = method;
							}
						} else if (ty instanceof CardDefinition) {
							
						} else 
							throw new NotImplementedException();
					}
				} else if (ty instanceof ContractDecl) {
					ContractDecl cd = (ContractDecl) ty;
					e = cd.getMethod(uv.var);
				} else if (ty instanceof ApplicationRouting) {
					// I'm not sure if this is just "too hard for me right now"
					// or "too hard".
					// I'm not convinced that it can be analyzed statically.
					return;
				} else
					throw new HaventConsideredThisException("need to handle " + ty.getClass());
				if (e != null)
					((UnresolvedVar)fld).bind(e);
				
				// if we still can't find e, there is a good chance that is because it isn't there:
				// the user may have typed something erroneous
			}
		}
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
