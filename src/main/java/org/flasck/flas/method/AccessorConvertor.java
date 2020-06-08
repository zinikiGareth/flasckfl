package org.flasck.flas.method;

import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.AccessorHolder;
import org.flasck.flas.parsedForm.FieldAccessor;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.MakeSend;
import org.flasck.flas.parsedForm.ObjectCtor;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.StateHolder;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TemplateBindingOption;
import org.flasck.flas.parsedForm.TemplateNestedField;
import org.flasck.flas.parsedForm.TemplateStylingOption;
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
import org.flasck.flas.tc3.PolyInstance;
import org.flasck.flas.tc3.Type;
import org.zinutils.exceptions.CantHappenException;
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
	public void leaveMemberExpr(MemberExpr expr) {
//		sv.result(null);
	}
	
	@Override
	public void postUnitTestAssert(UnitTestAssert a) {
		sv.result(null);
	}

	@Override
	public void leaveTemplateBindingOption(TemplateBindingOption option) {
		sv.result(null);
	}

	@Override
	public void leaveTemplateStyling(TemplateStylingOption option) {
		sv.result(null);
	}

	@Override
	public void visitMemberExpr(MemberExpr expr) {
		UnresolvedVar meth = (UnresolvedVar) expr.fld;
		Expr from = expr.from;
		RepositoryEntry defn;
		if (from instanceof UnresolvedVar) {
			UnresolvedVar uv = (UnresolvedVar) expr.from;
			defn = uv.defn();
		} else if (from instanceof MemberExpr) {
			MemberExpr me = (MemberExpr) expr.from;
			defn = me.defn();
		} else
			throw new NotImplementedException("cannot handle member of " + from.getClass());
		AccessorHolder ah;
		if (defn instanceof UnitDataDeclaration) {
			UnitDataDeclaration udd = (UnitDataDeclaration) defn;
			NamedType td = udd.ofType.defn();
			if (td instanceof StateHolder) {
				// UDDs can prod state directly on cards, agents and objects ...
				StateHolder sh = (StateHolder)td;
				if (sh.state() != null && sh.state().hasMember(meth.var)) {
					expr.conversion(new ApplyExpr(expr.location, LoadBuiltins.probeState, expr.from, new StringLiteral(meth.location, meth.var)));
					return;
				}
			}
			RepositoryEntry entry = repository.get(FunctionName.function(meth.location, td.name(), meth.var).uniqueName());
			if (entry != null && entry instanceof FunctionDefinition) {
				UnresolvedVar call = new UnresolvedVar(meth.location, meth.var);
				call.bind(entry);
				expr.conversion(new ApplyExpr(expr.location, call, new ApplyExpr(expr.location, LoadBuiltins.getUnderlying, from)));
				return;
			}

			if (td instanceof AccessorHolder && ((AccessorHolder)td).getAccessor(meth.var) != null) 
				ah = (AccessorHolder) td;
			else if (td instanceof ObjectDefn && ((ObjectDefn)td).getMethod(meth.var) != null) {
				ObjectDefn od = (ObjectDefn) td;
				ObjectMethod m = od.getMethod(meth.var);
				expr.conversion(new MakeSend(expr.location(), m.name(), from, m.argCount(), null));
				return;
			} else {
				errors.message(meth.location, "there is no suitable value for '" + meth.var + "' on " + td.name().uniqueName());
				return;
			}
		} else if (defn instanceof ObjectDefn) {
			// it's actually a ctor not an accessor
			ObjectDefn od = (ObjectDefn) defn;
			ObjectCtor ctor = od.getConstructor(meth.var);
			if (ctor == null)
				throw new CantHappenException("no constructor " + ctor);
			UnresolvedVar cv = new UnresolvedVar(from.location(), meth.var);
			cv.bind(ctor);
			expr.conversion(cv);
			return;
		} else if (defn instanceof TypedPattern) {
			TypedPattern tp = (TypedPattern)defn;
			Type ty = tp.type();
			if (ty instanceof PolyInstance)
				ty = ((PolyInstance)ty).struct(); 
			ah = (AccessorHolder) ty;
		} else if (defn instanceof FunctionDefinition) {
			FunctionDefinition fn = (FunctionDefinition) defn;
			if (fn.argCount() != 0)
				throw new NotImplementedException("cannot extract object from " + defn.getClass() + " with " + fn.argCount());
			ah = (AccessorHolder) fn.type();
		} else if (defn instanceof StructField) {
			ah = (AccessorHolder) ((StructField)defn).type();
		} else if (defn instanceof TemplateNestedField) {
			ah = (AccessorHolder) ((TemplateNestedField)defn).type();
		} else
			throw new NotImplementedException("cannot extract object from " + defn.getClass());
		FieldAccessor acc = ah.getAccessor(meth.var);
		if (acc == null)
			errors.message(meth.location, "there is no accessor '" + meth.var + "' on " + ah.name().uniqueName());
		else
			expr.conversion(acc.acor(expr.from));
	}
}
