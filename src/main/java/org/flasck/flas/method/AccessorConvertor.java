package org.flasck.flas.method;

import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.MakeAcor;
import org.flasck.flas.parsedForm.ObjectAccessor;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;

public class AccessorConvertor extends LeafAdapter {
	private final ErrorReporter errors;

	public AccessorConvertor(NestedVisitor sv, ErrorReporter errors) {
		this.errors = errors;
		sv.push(this);
	}

	@Override
	public void visitMemberExpr(MemberExpr expr) {
		UnresolvedVar meth = (UnresolvedVar) expr.fld;
		UnresolvedVar uv = (UnresolvedVar) expr.from;
		UnitDataDeclaration udd = (UnitDataDeclaration) uv.defn();
		ObjectDefn od = (ObjectDefn) udd.ofType.defn();
		ObjectAccessor acc = od.getAccessor(meth.var);
		if (acc == null)
			errors.message(meth.location, "there is no accessor '" + meth.var + "' on " + od.name().uniqueName());
		else
			expr.conversion(new MakeAcor(null, acc.name(), expr.from, 0));
	}
}
