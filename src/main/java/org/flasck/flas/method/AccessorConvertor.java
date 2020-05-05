package org.flasck.flas.method;

import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.AccessorHolder;
import org.flasck.flas.parsedForm.FieldAccessor;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.StateHolder;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.ut.UnitTestAssert;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.tc3.NamedType;
import org.zinutils.exceptions.NotImplementedException;

public class AccessorConvertor extends LeafAdapter {
	private final NestedVisitor sv;
	private final ErrorReporter errors;
	private final RepositoryReader repository;

	public AccessorConvertor(NestedVisitor sv, ErrorReporter errors, RepositoryReader repository) {
		this.sv = sv;
		this.errors = errors;
		this.repository = repository;
		sv.push(this);
	}

	@Override
	public void leaveFunction(FunctionDefinition a) {
		sv.result(null);
	}
	
	@Override
	public void postUnitTestAssert(UnitTestAssert a) {
		sv.result(null);
	}
	
	@Override
	public void visitMemberExpr(MemberExpr expr) {
		UnresolvedVar meth = (UnresolvedVar) expr.fld;
		UnresolvedVar uv = (UnresolvedVar) expr.from;
		AccessorHolder od;
		if (uv.defn() instanceof UnitDataDeclaration) {
			UnitDataDeclaration udd = (UnitDataDeclaration) uv.defn();
			NamedType td = udd.ofType.defn();
			if (td instanceof StateHolder) {
				// UDDs can prod state directly on cards, agents and objects ...
				StateHolder sh = (StateHolder)td;
				if (sh.state() != null && sh.state().hasMember(meth.var)) {
					expr.conversion(new ApplyExpr(expr.location, LoadBuiltins.prodState, expr.from, new StringLiteral(meth.location, meth.var)));
					return;
				}
			}
			RepositoryEntry entry = repository.get(FunctionName.function(meth.location, td.name(), meth.var).uniqueName());
			if (entry != null && entry instanceof FunctionDefinition) {
				UnresolvedVar call = new UnresolvedVar(meth.location, meth.var);
				call.bind(entry);
				expr.conversion(new ApplyExpr(expr.location, call, uv));
				return;
			}

			if (td instanceof AccessorHolder && ((AccessorHolder)td).getAccessor(meth.var) != null) 
				od = (AccessorHolder) td;
			else {
				errors.message(meth.location, "there is no suitable value for '" + meth.var + "' on '" + td.name().uniqueName());
				return;
			}
		} else if (uv.defn() instanceof TypedPattern) {
			TypedPattern tp = (TypedPattern)uv.defn();
			od = (ObjectDefn) tp.type();
		} else if (uv.defn() instanceof FunctionDefinition) {
			FunctionDefinition fn = (FunctionDefinition) uv.defn();
			if (fn.argCount() != 0)
				throw new NotImplementedException("cannot extract object from " + uv.defn().getClass() + " with " + fn.argCount());
			od = (AccessorHolder) fn.type();
		} else
			throw new NotImplementedException("cannot extract object from " + uv.defn().getClass());
		FieldAccessor acc = od.getAccessor(meth.var);
		if (acc == null)
			errors.message(meth.location, "there is no accessor '" + meth.var + "' on " + od.name().uniqueName());
		else
			expr.conversion(acc.acor(expr.from));
	}
}
