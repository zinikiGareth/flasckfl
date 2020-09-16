package org.flasck.flas.method;

import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.lifting.NestedVarReader;
import org.flasck.flas.parsedForm.AccessorHolder;
import org.flasck.flas.parsedForm.FieldAccessor;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.MakeSend;
import org.flasck.flas.parsedForm.ObjectCtor;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.StateHolder;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TemplateBindingOption;
import org.flasck.flas.parsedForm.TemplateNestedField;
import org.flasck.flas.parsedForm.TemplateStylingOption;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
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
import org.zinutils.exceptions.HaventConsideredThisException;
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
	public boolean visitMemberExpr(MemberExpr expr, int nargs) {
		UnresolvedVar meth = (UnresolvedVar) expr.fld;
		Expr from = expr.from;
		RepositoryEntry defn;
		boolean ret = expr.boundEarly();
		if (ret) {
			defn = expr.defn();
			if (defn instanceof FunctionDefinition || defn instanceof StructDefn) {
				UnresolvedVar uv = new UnresolvedVar(expr.location, "expr");
				uv.bind(defn);
				expr.conversion(uv);
			} else
				throw new HaventConsideredThisException("was not a function");
			return true;
		} else if (from instanceof UnresolvedVar) {
			UnresolvedVar uv = (UnresolvedVar) expr.from;
			defn = uv.defn();
		} else if (from instanceof MemberExpr) {
			MemberExpr me = (MemberExpr) expr.from;
			defn = me.defn();
		} else if (from instanceof ApplyExpr) { // and possibly other cases ...
			defn = (RepositoryEntry) expr.containerType(); // the TypeChecker figured out what the containing type is already
		} else
			throw new NotImplementedException("cannot handle member of " + from.getClass());
//		List<Type> polys;
		if (defn instanceof PolyInstance) {
			PolyInstance pi = (PolyInstance) defn;
			defn = (RepositoryEntry) pi.struct();
			// I feel we should need this in order to reconstruct the actual type later ...
//			polys = pi.getPolys();
		}
		AccessorHolder ah;
		if (defn instanceof UnitDataDeclaration) {
			UnitDataDeclaration udd = (UnitDataDeclaration) defn;
			NamedType td = udd.ofType.defn();
			if (td instanceof StateHolder) {
				// UDDs can prod state directly on cards, agents and objects ...
				StateHolder sh = (StateHolder)td;
				if (sh.state() != null && sh.state().hasMember(meth.var)) {
					expr.conversion(new ApplyExpr(expr.location, LoadBuiltins.probeState, expr.from, new StringLiteral(meth.location, meth.var)));
					return ret;
				}
			}
			RepositoryEntry entry = repository.get(FunctionName.function(meth.location, td.name(), meth.var).uniqueName());
			if (entry != null && entry instanceof FunctionDefinition) {
				UnresolvedVar call = new UnresolvedVar(meth.location, meth.var);
				call.bind(entry);
				expr.conversion(new ApplyExpr(expr.location, call, new ApplyExpr(expr.location, LoadBuiltins.getUnderlying, from)));
				return ret;
			}

			if (td instanceof AccessorHolder && ((AccessorHolder)td).getAccessor(meth.var) != null) 
				ah = (AccessorHolder) td;
			else if (td instanceof ObjectDefn && ((ObjectDefn)td).getMethod(meth.var) != null) {
				ObjectDefn od = (ObjectDefn) td;
				ObjectMethod m = od.getMethod(meth.var);
				expr.conversion(new MakeSend(expr.location(), m.name(), from, m.argCount(), null));
				return ret;
			} else {
				errors.message(meth.location, "there is no suitable value for '" + meth.var + "' on " + td.name().uniqueName());
				return ret;
			}
		} else if (defn instanceof StructDefn) {
			ah = (AccessorHolder) defn;
		} else if (defn instanceof ObjectDefn) {
			// it's actually a ctor not an accessor
			ObjectDefn od = (ObjectDefn) defn;
			ObjectCtor ctor = od.getConstructor(meth.var);
			if (ctor == null)
				throw new CantHappenException("no constructor " + ctor);
			UnresolvedVar cv = new UnresolvedVar(from.location(), meth.var);
			cv.bind(ctor);
			expr.conversion(cv);
			return ret;
		} else if (defn instanceof TypedPattern) {
			TypedPattern tp = (TypedPattern)defn;
			Type ty = tp.type();
			if (ty instanceof PolyInstance)
				ty = ((PolyInstance)ty).struct(); 
			ah = (AccessorHolder) ty;
		} else if (defn instanceof VarPattern) {
			Type ty = ((VarPattern)defn).type();
			if (ty == null) {
				throw new CantHappenException("type of " + defn + " has not been bound");
			}
			if (ty instanceof PolyInstance)
				ty = ((PolyInstance)ty).struct(); 
			ah = (AccessorHolder) ty;
		} else if (defn instanceof FunctionDefinition) {
			FunctionDefinition fn = (FunctionDefinition) defn;
			if (fn.argCountWithoutHolder() == 0) {
				if (fn.hasState())
					ah = (AccessorHolder) fn.type().get(1);
				else
					ah = (AccessorHolder) fn.type();
			} else {
				NestedVarReader nv = fn.nestedVars();
				if (nv.patterns().size() == fn.argCountWithoutHolder()) {
					// It feels like we need to apply the function to these arguments, but that isn't actually true, since it is done automatically
					// during code generation
					ah = (AccessorHolder) fn.type().get(nv.size() + (fn.hasState()?1:0));
				} else
					throw new NotImplementedException("cannot extract object from " + defn.getClass() + " with " + fn.argCount());
			}
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
		return ret;
	}
}
