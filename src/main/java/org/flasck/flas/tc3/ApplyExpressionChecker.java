package org.flasck.flas.tc3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.tc3.ExpressionChecker.ExprResult;
import org.flasck.flas.tc3.ExpressionChecker.IgnoreMe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplyExpressionChecker extends LeafAdapter implements ResultAware {
	public final static Logger logger = LoggerFactory.getLogger("TypeChecker");
	private final ErrorReporter errors;
	private final RepositoryReader repository;
	private final NestedVisitor nv;
	private final List<PosType> results = new ArrayList<>();
	private final CurrentTCState state;
	private final String fnCxt;
	private final boolean inTemplate;
	private Expr tmp;

	public ApplyExpressionChecker(ErrorReporter errors, RepositoryReader repository, CurrentTCState state, NestedVisitor nv, String fnCxt, boolean inTemplate) {
		this.errors = errors;
		this.repository = repository;
		this.state = state;
		this.nv = nv;
		this.fnCxt = fnCxt;
		this.inTemplate = inTemplate;
	}
	
	@Override
	public void visitExpr(Expr expr, int nArgs) {
		tmp = expr;
		nv.push(new ExpressionChecker(errors, repository, state, nv, fnCxt, inTemplate));
	}
	
	@Override
	public boolean visitMemberExpr(MemberExpr expr, int nargs) {
		tmp = expr;
		nv.push(new MemberExpressionChecker(errors, repository, state, nv, fnCxt, false));
		return expr.boundEarly();
	}
	
	@Override
	public void result(Object r) {
		if (r instanceof IgnoreMe)
			return;
		ExprResult ty = (ExprResult) r;
		if (ty == null || ty.type == null) {
			throw new NullPointerException("Cannot handle null type");
		}
		results.add(TypeChecker.instantiateFreshPolys(tmp, state, new TreeMap<>(), ty, false));
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
		if (expr.fn instanceof UnresolvedOperator && ((UnresolvedOperator)expr.fn).op.equals("{}")) {
			handleHashBuilder(expr);
			return;
		}
		PosType pfn = results.remove(0);
		Type fn = pfn.type;
		logger.debug("attempting to check application of " + expr.fn + " " + fn + " to " + results);
		if (fn instanceof ErrorType) {
			nv.result(fn);
			return;
		} else if (fn instanceof UnifiableType) {
			UnifiableType ut = (UnifiableType)fn;
			nv.result(ut.canBeAppliedTo(expr.location(), results));
			return;
		} else if (fn.argCount() < results.size()) {
			if (fn instanceof StructDefn && fn.argCount() == results.size()-1 && results.get(results.size()-1).type == LoadBuiltins.hash) {
				// this is the case where we try and override values in the struct with values in the hash
				// in the simplest case, this is obviously "just fine": you can do this and the check to get here *is* the typecheck
				// we just need to remove it so that we can test the rest.
				results.remove(results.size()-1);
				// TODO: we should look into the hash itself if possible and determine what keys it has ... 
			} else {
				errors.message(pfn.pos, expr.fn + " expects: " + fn.argCount() + " has: " + results.size());
				nv.result(new ErrorType());
				return;
			}
		}
		List<Type> tocurry = new ArrayList<>();
		int pos = 0;
		int max = fn.argCount();
		while (!results.isEmpty() && pos < max) {
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
			} else if (fi instanceof ErrorType || ai instanceof ErrorType) {
				nv.result(new ErrorType());
				return;
			} else if (!fi.incorporates(loc, ai)) {
				logger.info("TC Error: " + expr.fn + " was expecting " + fi.signature() + " not " + ai.signature());
				errors.message(loc, "function '" + expr.fn + "' was expecting " + fi.signature() + " not " + ai.signature());
				nv.result(new ErrorType());
				return;
			}
			pos++;
		}
		if (!results.isEmpty()) {
			PosType pai = results.remove(0);
			if (pai.type instanceof HandlerImplements) {
				errors.message(expr.location(), "unexpected handler as argument; did you forget '->' ?");
				return;
			} else {
				String name = "function '" + expr.fn;
				if (expr.fn instanceof MemberExpr)
					name = "method '" + ((MemberExpr)expr.fn).fld;
				errors.message(expr.location(), "excess arguments to " + name + "'");
				return;
			}
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

	private void handleHashBuilder(ApplyExpr expr) {
		if (expr.args.isEmpty()) {
			nv.result(LoadBuiltins.hash);
		} else {
			results.remove(0); // remove the hash from the front
			// I think our parser should guarantee this can't happen ...
			for (PosType e : results) {
				if (e.type != LoadBuiltins.hashPairType) {
					errors.message(e.pos, "invalid entry in hash: " + e.type.signature());
					nv.result(new ErrorType());
					return;
				}
			}
			nv.result(LoadBuiltins.hash);
		}
	}
}
