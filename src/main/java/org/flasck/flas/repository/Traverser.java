package org.flasck.flas.repository;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.hsi.ArgSlot;
import org.flasck.flas.hsi.HSIVisitor;
import org.flasck.flas.hsi.Slot;
import org.flasck.flas.parsedForm.ConstructorMatch;
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
import org.flasck.flas.patterns.HSIOptions;
import org.flasck.flas.patterns.HSITree;
import org.flasck.flas.repository.Repository.Visitor;
import org.flasck.flas.tc3.Primitive;
import org.zinutils.exceptions.NotImplementedException;

public class Traverser implements Visitor {
	private final Visitor visitor;

	public Traverser(Visitor visitor) {
		this.visitor = visitor;
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
		if (visitor instanceof HSIVisitor) {
			List<Slot> slots = new ArrayList<>();
			for (int i=0;i<fn.argCount();i++) {
				slots.add(new ArgSlot(i, fn.hsiTree().get(i)));
			}
			((HSIVisitor)visitor).hsiArgs(slots);
			visitHSI(fn, slots, fn.intros());
		} else {
			for (FunctionIntro i : fn.intros())
				visitFunctionIntro(i);
		}
		visitor.leaveFunction(fn);
	}

	public void visitHSI(FunctionDefinition fn, List<Slot> slots, List<FunctionIntro> intros) {
		HSIVisitor hsi = (HSIVisitor) visitor;
		if (slots.isEmpty()) {
			if (intros.size() == 1)
				handleInline(hsi, intros.get(0));
			else
				throw new NotImplementedException("I think this is an error");
		} else {
			Slot s = selectSlot(slots);
			List<Slot> remaining = new ArrayList<>(slots);
			remaining.remove(s);
			HSIOptions opts = s.getOptions();
			if (opts.hasSwitches()) {
				hsi.switchOn(s);
				for (String c : opts.ctors()) {
					hsi.withConstructor(c);
					HSITree cm = opts.getCM(c);
					if (cm.intros().size() != 1)
						throw new NotImplementedException();
					visitHSI(fn, remaining, cm.intros());
				}
				for (String ty : opts.types()) {
					hsi.withConstructor(ty);
					visitHSI(fn, remaining, opts.getIntrosForType(ty));
				}
			} else {
				for (VarName v : opts.vars())
					hsi.bind(s, v.var);
				visitHSI(fn, remaining, intros);
			}
			if (opts.hasSwitches()) {
				hsi.errorNoCase();
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

	private void handleInline(HSIVisitor hsi, FunctionIntro i) {
		hsi.startInline(i);
		for (FunctionCaseDefn c : i.cases())
			visitCase(c);
		hsi.endInline(i);
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

	public void visitConstructorMatch(ConstructorMatch p) {
		visitor.visitConstructorMatch(p);
		leaveConstructorMatch(p);
	}

	public void leaveConstructorMatch(ConstructorMatch p) {
		visitor.leaveConstructorMatch(p);
	}

	public void visitPatternVar(InputPosition varLoc, String var) {
		visitor.visitPatternVar(varLoc, var);
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
		visitExpr((Expr) expr.fn, expr.args.size());
		for (Object x : expr.args)
			visitExpr((Expr) x, 0);
		visitor.leaveApplyExpr(expr);
	}

	@Override
	public void leaveApplyExpr(ApplyExpr expr) {
	}

	@Override
	public void visitUnresolvedVar(UnresolvedVar var, int nargs) {
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
		visitExpr(a.value, 0);
		visitExpr(a.expr, 0);
		visitor.postUnitTestAssert(a);
	}

	@Override
	public void postUnitTestAssert(UnitTestAssert a) {
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
