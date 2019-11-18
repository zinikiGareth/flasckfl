package org.flasck.flas.tc3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FieldsDefn;
import org.flasck.flas.parsedForm.PolyHolder;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.tc3.ExpressionChecker.ExprResult;

public class ApplyExpressionChecker extends LeafAdapter implements ResultAware {
	private final ErrorReporter errors;
	private final NestedVisitor nv;
	private final List<Type> results = new ArrayList<>();
	private final CurrentTCState state;

	public ApplyExpressionChecker(ErrorReporter errors, CurrentTCState state, NestedVisitor nv) {
		this.errors = errors;
		this.state = state;
		this.nv = nv;
	}
	
	@Override
	public void visitExpr(Expr expr, int nArgs) {
		nv.push(new ExpressionChecker(errors, state, nv));
	}
	
	@Override
	public void visitMemberExpr(MemberExpr expr) {
		nv.push(new MemberExpressionChecker(errors, state, nv));
	}
	
	@Override
	public void result(Object r) {
		Type ty = ((ExprResult) r).type;
		if (ty == null) {
			throw new NullPointerException("Cannot handle null type");
		}
		results.add(instantiateFreshPolys(new TreeMap<>(), ty));
	}

	public Type instantiateFreshPolys(Map<PolyType, UnifiableType> uts, Type type) {
		if (type instanceof PolyType) {
			PolyType pt = (PolyType) type;
			if (uts.containsKey(pt))
				return uts.get(pt);
			else {
				UnifiableType ret = state.createUT(null);
				uts.put(pt, ret);
				return ret;
			}
		} else if (type instanceof Apply) {
			Apply a = (Apply) type;
			List<Type> types = new ArrayList<>();
			for (Type t : a.tys)
				types.add(instantiateFreshPolys(uts, t));
			return new Apply(types);
		} else if (type instanceof PolyHolder && ((PolyHolder)type).hasPolys()) {
			PolyHolder sd = (PolyHolder) type;
			List<Type> polys = new ArrayList<>();
			for (Type t : sd.polys())
				polys.add(instantiateFreshPolys(uts, t));
			PolyInstance pi = new PolyInstance(sd, polys);
			if (type instanceof FieldsDefn) {
				List<Type> types = new ArrayList<>();
				for (StructField sf : ((FieldsDefn)type).fields)
					types.add(instantiateFreshPolys(uts, sf.type.defn()));
				if (types.isEmpty())
					return pi;
				else
					return new Apply(types, pi);
			} else {
				return pi; 
			}
		} else
			return type;
	}

	@Override
	public void leaveApplyExpr(ApplyExpr expr) {
		if (expr.fn instanceof UnresolvedOperator && ((UnresolvedOperator)expr.fn).op.equals("[]")) {
			handleListBuilder(expr);
			return;
		}
		// TODO: we may need to explicitly handle the case where we have a "Send" constructor that wants to collapse a set of arguments into a list
		// But if we return the right contract method type, it may all just go swimmingly
		// And we will just have that concern in MethodConversion
		Type fn = results.remove(0);
		if (fn instanceof UnifiableType) {
			UnifiableType ut = (UnifiableType)fn;
			nv.result(ut.canBeAppliedTo(results));
			return;
		} else if (fn.argCount() < results.size())
			throw new RuntimeException("should be an error: " + fn + " expects: " + fn.argCount() + " has: " + results.size());
		List<Type> tocurry = new ArrayList<>();
		int pos = 0;
		while (!results.isEmpty()) {
			Type ai = results.remove(0);
			if (ai instanceof ErrorType) {
				nv.result(ai);
				return;
			}
			InputPosition loc = ((Locatable)expr.args.get(pos)).location();
			Type fi = fn.get(pos);
			if (ai instanceof CurryArgumentType) {
				tocurry.add(fi);
			} else if (!fi.incorporates(loc, ai)) {
				errors.message(loc, "function '" + expr.fn + "' was expecting " + fi.signature() + " not " + ai.signature());
				nv.result(new ErrorType());
				return;
			}
			pos++;
		}
		// anything left must be curried
		while (pos < fn.argCount()) {
			tocurry.add(fn.get(pos++));
		}
		// if we have any curried args, we need to make an apply
		if (!tocurry.isEmpty()) {
			tocurry.add(fn.get(pos));
			nv.result(new Apply(tocurry));
		} else
			nv.result(fn.get(pos));
	}

	private void handleListBuilder(ApplyExpr expr) {
		if (expr.args.isEmpty()) {
			nv.result(LoadBuiltins.nil);
		} else {
			results.remove(0); // remove the nil from the front
			Type ty = state.consolidate(expr.location(), results);
			nv.result(new PolyInstance(LoadBuiltins.cons, Arrays.asList(ty)));
		}
	}
}
