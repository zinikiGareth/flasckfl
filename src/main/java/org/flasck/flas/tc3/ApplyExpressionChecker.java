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
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.FieldsDefn;
import org.flasck.flas.parsedForm.PolyHolder;
import org.flasck.flas.parsedForm.PolyType;
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
	private final List<PosType> results = new ArrayList<>();
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
		ExprResult ty = (ExprResult) r;
		if (ty == null || ty.type == null) {
			throw new NullPointerException("Cannot handle null type");
		}
		results.add(instantiateFreshPolys(new TreeMap<>(), ty));
	}

	public PosType instantiateFreshPolys(Map<PolyType, UnifiableType> uts, PosType post) {
		InputPosition pos = post.pos;
		Type type = post.type;
		if (type instanceof PolyType) {
			PolyType pt = (PolyType) type;
			if (uts.containsKey(pt))
				return new PosType(pos, uts.get(pt));
			else {
				UnifiableType ret = state.createUT(null, "instantiating " + pt.shortName());
				uts.put(pt, ret);
				return new PosType(pos, ret);
			}
		} else if (type instanceof Apply) {
			Apply a = (Apply) type;
			List<Type> types = new ArrayList<>();
			for (Type t : a.tys)
				types.add(instantiateFreshPolys(uts, new PosType(pos, t)).type);
			return new PosType(pos, new Apply(types));
		} else if (type instanceof PolyHolder && ((PolyHolder)type).hasPolys()) {
			PolyHolder sd = (PolyHolder) type;
			List<Type> polys = new ArrayList<>();
			for (Type t : sd.polys())
				polys.add(instantiateFreshPolys(uts, new PosType(pos, t)).type);
			PolyInstance pi = new PolyInstance(pos, sd, polys);
			if (type instanceof FieldsDefn) {
				List<Type> types = new ArrayList<>();
				for (StructField sf : ((FieldsDefn)type).fields)
					types.add(instantiateFreshPolys(uts, new PosType(pos, sf.type.defn())).type);
				if (types.isEmpty())
					return new PosType(pos, pi);
				else
					return new PosType(pos, new Apply(types, pi));
			} else {
				return new PosType(pos, pi);
			}
		} else
			return post;
	}

	@Override
	public void leaveApplyExpr(ApplyExpr expr) {
		if (expr.fn instanceof UnresolvedOperator && ((UnresolvedOperator)expr.fn).op.equals("[]")) {
			handleListBuilder(expr);
			return;
		}
		if (expr.fn instanceof UnresolvedOperator && ((UnresolvedOperator)expr.fn).op.equals("()")) {
			handleTupleBuilder(expr);
			return;
		}
		PosType pfn = results.remove(0);
		Type fn = pfn.type;
		if (fn instanceof ErrorType) {
			nv.result(fn);
			return;
		} else if (fn instanceof UnifiableType) {
			UnifiableType ut = (UnifiableType)fn;
			nv.result(ut.canBeAppliedTo(expr.location(), results));
			return;
		} else if (fn.argCount() < results.size()) {
			errors.message(pfn.pos, fn + " expects: " + fn.argCount() + " has: " + results.size());
			nv.result(new ErrorType());
			return;
		}
		List<Type> tocurry = new ArrayList<>();
		int pos = 0;
		while (!results.isEmpty()) {
			PosType pai = results.remove(0);
			Type ai = pai.type;
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
		if (unusedHandlerCase(expr.fn)) {
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

	private boolean unusedHandlerCase(Object fn) {
		if (!(fn instanceof MemberExpr))
			return false;
		MemberExpr me = (MemberExpr) fn;
		ContractMethodDecl cmd = me.contractMethod();
		if (cmd == null)
			return false;
		// TODO: There are subcases here around the specification and use of the handler
		return true;
	}

	private void handleListBuilder(ApplyExpr expr) {
		if (expr.args.isEmpty()) {
			nv.result(LoadBuiltins.nil);
		} else {
			results.remove(0); // remove the nil from the front
			PosType ty = state.consolidate(expr.location(), results);
			nv.result(new PolyInstance(expr.location(), LoadBuiltins.cons, Arrays.asList(ty.type)));
		}
	}

	private void handleTupleBuilder(ApplyExpr expr) {
		if (expr.args.size() < 2) {
			throw new RuntimeException("Tuples must have at least two elements");
		} else {
			results.remove(0); // remove the operator from the front
			List<Type> tys = new ArrayList<Type>();
			for (PosType pt : results)
				tys.add(pt.type);
			nv.result(new PolyInstance(expr.location(), LoadBuiltins.tuple, tys));
		}
	}
}
