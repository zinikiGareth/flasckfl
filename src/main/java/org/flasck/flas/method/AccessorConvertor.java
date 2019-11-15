package org.flasck.flas.method;

import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.MakeAcor;
import org.flasck.flas.parsedForm.ObjectAccessor;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.ut.UnitTestAssert;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.zinutils.exceptions.NotImplementedException;

public class AccessorConvertor extends LeafAdapter {
	private final NestedVisitor sv;
	private final ErrorReporter errors;

	public AccessorConvertor(NestedVisitor sv, ErrorReporter errors) {
		this.sv = sv;
		this.errors = errors;
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
		ObjectDefn od;
		if (uv.defn() instanceof UnitDataDeclaration) {
			UnitDataDeclaration udd = (UnitDataDeclaration) uv.defn();
			od = (ObjectDefn) udd.ofType.defn();
		} else if (uv.defn() instanceof TypedPattern) {
			TypedPattern tp = (TypedPattern)uv.defn();
			od = (ObjectDefn) tp.type();
		} else
			throw new NotImplementedException("cannot extract object from " + uv.defn().getClass());
		ObjectAccessor acc = od.getAccessor(meth.var);
		if (acc == null)
			errors.message(meth.location, "there is no accessor '" + meth.var + "' on " + od.name().uniqueName());
		else
			expr.conversion(new MakeAcor(null, acc.name(), expr.from, acc.function().argCount()));
	}
}
