package org.flasck.flas.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.ConstPattern;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.hsi.CMSlot;
import org.flasck.flas.hsi.HSIVisitor;
import org.flasck.flas.hsi.Slot;
import org.flasck.flas.lifting.NestedVarReader;
import org.flasck.flas.parsedForm.ConstructorMatch;
import org.flasck.flas.parsedForm.ConstructorMatch.Field;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnionTypeDefn;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.parsedForm.ut.UnitTestAssert;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.parsedForm.ut.UnitTestPackage;
import org.flasck.flas.parsedForm.ut.UnitTestStep;
import org.flasck.flas.patterns.HSICtorTree;
import org.flasck.flas.patterns.HSIOptions;
import org.flasck.flas.patterns.HSIOptions.IntroVarName;
import org.flasck.flas.repository.Repository.Visitor;
import org.flasck.flas.tc3.Primitive;
import org.zinutils.exceptions.NotImplementedException;

public class Traverser implements Visitor {
	private final Visitor visitor;
	private FunctionDefinition currentFunction;

	public Traverser(Visitor visitor) {
		this.visitor = visitor;
	}

	@Override
	public boolean isHsi() {
		return visitor.isHsi();
	}

	/** It's starting to concern me that for some things (contracts, unit tests) we visit
	 * the parent object and then all of its children, but for other things,
	 * such as objects and their methods, we view both as being in the repository and allow
	 * the repository to do its work in any order.
	 */
	@Override
	public void visitEntry(RepositoryEntry e) {
		if (e == null)
			throw new org.zinutils.exceptions.NotImplementedException("traverser cannot handle null entries");
		else if (e instanceof Primitive)
			visitPrimitive((Primitive)e);
		else if (e instanceof BuiltinRepositoryEntry)
			; // do nothing for builtins
		else if (e instanceof ContractDecl)
			visitContractDecl((ContractDecl)e);
		else if (e instanceof ObjectDefn)
			visitObjectDefn((ObjectDefn)e);
		else if (e instanceof FunctionDefinition)
			visitFunction((FunctionDefinition)e);
		else if (e instanceof ObjectMethod)
			visitObjectMethod((ObjectMethod)e);
		else if (e instanceof StructDefn)
			visitStructDefn((StructDefn)e);
		else if (e instanceof UnionTypeDefn)
			visitUnionTypeDefn((UnionTypeDefn)e);
		else if (e instanceof UnitTestPackage)
			visitUnitTestPackage((UnitTestPackage)e);
		else if (e instanceof VarPattern || e instanceof TypedPattern)
			; // do nothing: it is just in the repo for lookup purposes
		else
			throw new org.zinutils.exceptions.NotImplementedException("traverser cannot handle " + e.getClass());
	}

	@Override
	public void visitPrimitive(Primitive p) {
		visitor.visitPrimitive(p);
	}

	@Override
	public void visitStructDefn(StructDefn s) {
		visitor.visitStructDefn(s);
		for (StructField f : s.fields)
			visitStructField(f);
		leaveStructDefn(s);
	}
	
	@Override
	public void visitStructField(StructField sf) {
		visitor.visitStructField(sf);
	}

	@Override
	public void leaveStructDefn(StructDefn s) {
		visitor.leaveStructDefn(s);
	}

	@Override
	public void visitUnionTypeDefn(UnionTypeDefn ud) {
		visitor.visitUnionTypeDefn(ud);
//		for (StructField f : s.fields)
//			visitStructField(f);
		leaveUnionTypeDefn(ud);
	}
	
	@Override
	public void leaveUnionTypeDefn(UnionTypeDefn ud) {
		visitor.leaveUnionTypeDefn(ud);
	}

	@Override
	public void visitObjectDefn(ObjectDefn e) {
		visitor.visitObjectDefn(e);
	}

	@Override
	public void visitObjectMethod(ObjectMethod e) {
		visitor.visitObjectMethod(e);
	}

	@Override
	public void visitFunction(FunctionDefinition fn) {
		if (fn.intros().isEmpty())
			return; // not for generation
		visitor.visitFunction(fn);
		if (visitor.isHsi()) {
			rememberCaller(fn);
			List<Slot> slots = fn.slots();
			((HSIVisitor)visitor).hsiArgs(slots);
			visitHSI(fn, new VarMapping(), slots, fn.intros());
			rememberCaller(null);
		} else {
			for (FunctionIntro i : fn.intros())
				visitFunctionIntro(i);
		}
		visitor.leaveFunction(fn);
	}
	
	public void rememberCaller(FunctionDefinition fn) {
		this.currentFunction = fn;
	}

	private static class SlotVar {
		private final Slot s;
		private final VarName var;

		public SlotVar(Slot s, VarName var) {
			this.s = s;
			this.var = var;
		}
	}
	
	public static class VarMapping {
		private Map<FunctionIntro, List<SlotVar>> map = new HashMap<>();
		
		public VarMapping remember(Slot s, HSIOptions opts, List<FunctionIntro> intros) {
			VarMapping ret = new VarMapping();
			ret.map.putAll(map);
			for (IntroVarName v : opts.vars(intros)) {
				if (!ret.map.containsKey(v.intro))
					ret.map.put(v.intro, new ArrayList<>());
				ret.map.get(v.intro).add(new SlotVar(s, v.var));
			}
			return ret;
		}

		public void bindFor(HSIVisitor hsi, FunctionIntro intro) {
			List<SlotVar> vars = map.get(intro);
			if (vars != null) {
				for (SlotVar sv : vars)
					hsi.bind(sv.s, sv.var.var);
			}
		}
	}

	public void visitHSI(FunctionDefinition fn, VarMapping vars, List<Slot> slots, List<FunctionIntro> intros) {
		HSIVisitor hsi = (HSIVisitor) visitor;
		if (slots.isEmpty()) {
			if (intros.isEmpty())
				hsi.errorNoCase();
			else if (intros.size() == 1) {
				FunctionIntro intro = intros.get(0);
				vars.bindFor(hsi, intro);
				handleInline(intro);
			} else
				throw new NotImplementedException("I think this is an error");
		} else {
			Slot s = selectSlot(slots);
			List<Slot> remaining = new ArrayList<>(slots);
			remaining.remove(s);
			HSIOptions opts = s.getOptions();
			vars = vars.remember(s, opts, intros);
			boolean wantSwitch = opts.hasSwitches(intros);
			if (wantSwitch) {
				hsi.switchOn(s);
				for (String c : opts.ctors()) {
					hsi.withConstructor(c);
					HSICtorTree cm = (HSICtorTree) opts.getCM(c);
					List<Slot> extended = new ArrayList<>(remaining);
					for (int i=0;i<cm.width();i++) {
						String fld = cm.getField(i);
						HSIOptions oi = cm.get(i);
						CMSlot fieldSlot = new CMSlot(oi);
						hsi.constructorField(s, fld, fieldSlot);
						extended.add(fieldSlot);
					}
					ArrayList<FunctionIntro> intersect = new ArrayList<>(intros);
					intersect.retainAll(cm.intros());
					visitHSI(fn, vars, extended, intersect);
				}
				for (String ty : opts.types(intros)) {
					hsi.withConstructor(ty);
					ArrayList<FunctionIntro> intersect = new ArrayList<>(intros);
					intersect.retainAll(opts.getIntrosForType(ty));
					if ("Number".equals(ty)) {
						Set<Integer> numbers = opts.numericConstants(intersect);
						if (!numbers.isEmpty()) {
							for (int k : numbers) {
								hsi.matchNumber(k);
								ArrayList<FunctionIntro> forConst = new ArrayList<>(intersect);
								forConst.retainAll(opts.getIntrosForType(ty));
								visitHSI(fn, vars, remaining, intersect);
								intersect.removeAll(forConst);
							}
							hsi.matchDefault();
						}
					}
					if ("String".equals(ty)) {
						Set<String> strings = opts.stringConstants(intersect);
						if (!strings.isEmpty()) {
							for (String k : strings) {
								hsi.matchString(k);
								ArrayList<FunctionIntro> forConst = new ArrayList<>(intersect);
								forConst.retainAll(opts.getIntrosForType(ty));
								visitHSI(fn, vars, remaining, intersect);
								intersect.removeAll(forConst);
							}
							hsi.matchDefault();
						}
					}
					if (intersect.isEmpty())
						hsi.errorNoCase();
					else
						visitHSI(fn, vars, remaining, intersect);
				}
			}
			ArrayList<FunctionIntro> intersect = new ArrayList<>(intros);
			intersect.retainAll(opts.getDefaultIntros(intros));
			if (wantSwitch)
				hsi.defaultCase();
			visitHSI(fn, vars, remaining, intersect);
			if (wantSwitch) {
				hsi.endSwitch();
			}
		}
	}
	
	public static Slot selectSlot(List<Slot> slots) {
		if (slots.size() == 1)
			return slots.get(0);
		int which = 0;
		int score = -1;
		int i = 0;
		for (Slot s : slots) {
			int ms = s.score();
			if (ms > score) {
				which = i;
				score = ms;
			}
			i++;
		}
		return slots.get(which);
	}

	private void handleInline(FunctionIntro i) {
		startInline(i);
		for (FunctionCaseDefn c : i.cases())
			visitCase(c);
		endInline(i);
	}

	@Override
	public void visitFunctionIntro(FunctionIntro i) {
		visitor.visitFunctionIntro(i);
		for (Object p : i.args)
			visitPattern(p);
		for (FunctionCaseDefn c : i.cases())
			visitCase(c);
		leaveFunctionIntro(i);
	}

	
	@Override
	public void leaveFunctionIntro(FunctionIntro fi) {
		visitor.leaveFunctionIntro(fi);
	}

	@Override
	public void leaveFunction(FunctionDefinition fn) {
	}

	@Override
	public void visitPattern(Object p) {
		visitor.visitPattern(p);
		if (p instanceof VarPattern)
			visitVarPattern((VarPattern) p);
		else if (p instanceof TypedPattern)
			visitTypedPattern((TypedPattern)p);
		else if (p instanceof ConstructorMatch)
			visitConstructorMatch((ConstructorMatch)p);
		else if (p instanceof ConstPattern)
			visitConstPattern((ConstPattern)p);
		else
			throw new org.zinutils.exceptions.NotImplementedException("Pattern not handled: " + p.getClass());
		leavePattern(p);
	}

	@Override
	public void visitVarPattern(VarPattern p) {
		visitor.visitVarPattern(p);
		visitPatternVar(p.varLoc, p.var);
	}

	@Override
	public void visitTypedPattern(TypedPattern p) {
		visitor.visitTypedPattern(p);
		visitTypeReference(p.type);
		visitPatternVar(p.var.loc, p.var.var);
	}

	@Override
	public void visitConstructorMatch(ConstructorMatch p) {
		visitor.visitConstructorMatch(p);
		for (Field f : p.args) {
			visitConstructorField(f.field, f.patt);
		}
		leaveConstructorMatch(p);
	}

	@Override
	public void visitConstructorField(String field, Object patt) {
		visitor.visitConstructorField(field, patt);
		visitPattern(patt);
		leaveConstructorField(field, patt);
	}

	@Override
	public void leaveConstructorField(String field, Object patt) {
		visitor.leaveConstructorField(field, patt);
	}

	@Override
	public void leaveConstructorMatch(ConstructorMatch p) {
		visitor.leaveConstructorMatch(p);
	}

	public void visitPatternVar(InputPosition varLoc, String var) {
		visitor.visitPatternVar(varLoc, var);
	}

	@Override
	public void visitConstPattern(ConstPattern p) {
		visitor.visitConstPattern(p);
	}

	@Override
	public void leavePattern(Object patt) {
		visitor.leavePattern(patt);
	}

	@Override
	public void visitCase(FunctionCaseDefn c) {
		if (c.guard != null)
			visitExpr(c.guard, 0);
		visitExpr(c.expr, 0);
	}

	@Override
	public void startInline(FunctionIntro fi) {
		visitor.startInline(fi);
	}

	@Override
	public void endInline(FunctionIntro fi) {
		visitor.endInline(fi);
	}

	@Override
	public void visitExpr(Expr expr, int nargs) {
		visitor.visitExpr(expr, nargs);
		if (expr == null)
			return;
		else if (expr instanceof ApplyExpr)
			visitApplyExpr((ApplyExpr)expr);
		else if (expr instanceof StringLiteral)
			visitStringLiteral((StringLiteral)expr);
		else if (expr instanceof NumericLiteral)
			visitNumericLiteral((NumericLiteral)expr);
		else if (expr instanceof UnresolvedVar)
			visitUnresolvedVar((UnresolvedVar) expr, nargs);
		else if (expr instanceof UnresolvedOperator)
			visitUnresolvedOperator((UnresolvedOperator) expr, nargs);
		else
			throw new org.zinutils.exceptions.NotImplementedException("Not handled: " + expr.getClass());
	}

	public void visitApplyExpr(ApplyExpr expr) {
		visitor.visitApplyExpr(expr);
		int cnt = expr.args.size();
		Expr fn = (Expr) expr.fn;
		NestedVarReader nv = null;
		if (visitor.isHsi()) {
			nv = isFnNeedingNesting(fn);
			if (nv != null)
				cnt += nv.vars().size();
		}
		visitExpr(fn, cnt);
		if (nv != null) {
			for (UnresolvedVar uv : nv.vars())
				visitExpr(uv, 0);
		}
		for (Object x : expr.args)
			visitExpr((Expr) x, 0);
		visitor.leaveApplyExpr(expr);
	}

	private NestedVarReader isFnNeedingNesting(Expr uv) {
		if (uv instanceof UnresolvedVar) {
			UnresolvedVar fn = (UnresolvedVar)uv;
			if (fn.defn() instanceof FunctionDefinition)
				return ((FunctionDefinition)fn.defn()).nestedVars();
		}
		return null;
	}

	@Override
	public void leaveApplyExpr(ApplyExpr expr) {
	}

	@Override
	public void visitUnresolvedVar(UnresolvedVar var, int nargs) {
		if (nargs == 0 && visitor.isHsi()) {
			NestedVarReader nv = isFnNeedingNesting(var);
			if (nv != null) {
				@SuppressWarnings({ "rawtypes", "unchecked" })
				List<Object> args = (List)nv.vars();
				ApplyExpr ae = new ApplyExpr(var.location, var, args);
				visitor.visitApplyExpr(ae);
				visitor.visitExpr(var, args.size());
				visitor.visitUnresolvedVar(var, args.size());
				for (Object v : args) {
					visitor.visitExpr((Expr) v, 0);
					visitor.visitUnresolvedVar((UnresolvedVar) v, 0);
				}
				visitor.leaveApplyExpr(ae);
				return; // don't just visit the var ...
			}
		}
		visitor.visitUnresolvedVar(var, nargs);
	}

	@Override
	public void visitUnresolvedOperator(UnresolvedOperator operator, int nargs) {
		visitor.visitUnresolvedOperator(operator, nargs);
	}

	@Override
	public void visitTypeReference(TypeReference var) {
		visitor.visitTypeReference(var);
	}

	@Override
	public void visitStringLiteral(StringLiteral expr) {
		visitor.visitStringLiteral(expr);
	}

	@Override
	public void visitNumericLiteral(NumericLiteral expr) {
		visitor.visitNumericLiteral(expr);
	}

	@Override
	public void visitUnitTestPackage(UnitTestPackage e) {
		visitor.visitUnitTestPackage(e);
		for (UnitTestCase c : e.tests())
			visitUnitTest(c);
		visitor.leaveUnitTestPackage(e);
	}

	@Override
	public void visitUnitTest(UnitTestCase e) {
		visitor.visitUnitTest(e);
		for (UnitTestStep s : e.steps) {
			visitUnitTestStep(s);
		}
		visitor.leaveUnitTest(e);
	}

	@Override
	public void leaveUnitTest(UnitTestCase e) {
	}

	@Override
	public void visitUnitTestStep(UnitTestStep s) {
		visitor.visitUnitTestStep(s);
		if (s instanceof UnitTestAssert)
			visitUnitTestAssert((UnitTestAssert) s);
		else
			throw new NotImplementedException();
	}

	@Override
	public void visitUnitTestAssert(UnitTestAssert a) {
		visitor.visitUnitTestAssert(a);
		visitAssertExpr(true, a.value);
		visitExpr(a.value, 0);
		leaveAssertExpr(true, a.value);
		visitAssertExpr(false, a.expr);
		visitExpr(a.expr, 0);
		leaveAssertExpr(false, a.expr);
		postUnitTestAssert(a);
	}

	@Override
	public void visitAssertExpr(boolean isValue, Expr e) {
		visitor.visitAssertExpr(isValue, e);
	}

	@Override
	public void leaveAssertExpr(boolean isValue, Expr e) {
		visitor.leaveAssertExpr(isValue, e);
	}

	@Override
	public void postUnitTestAssert(UnitTestAssert a) {
		visitor.postUnitTestAssert(a);
	}

	@Override
	public void leaveUnitTestPackage(UnitTestPackage e) {
	}

	@Override
	public void visitContractDecl(ContractDecl cd) {
		visitor.visitContractDecl(cd);
		for (ContractMethodDecl m : cd.methods)
			visitContractMethod(m);
		visitor.leaveContractDecl(cd);
	}

	@Override
	public void visitContractMethod(ContractMethodDecl cmd) {
		visitor.visitContractMethod(cmd);
	}

	@Override
	public void leaveContractDecl(ContractDecl cd) {
	}
}
