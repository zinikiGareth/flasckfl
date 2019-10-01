package org.flasck.flas.repository;

import java.util.LinkedList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
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
import org.flasck.flas.repository.Repository.Visitor;
import org.flasck.flas.tc3.Primitive;

public class StackVisitor implements NestedVisitor {
	private List<Visitor> stack = new LinkedList<>();
	private Visitor top;
	
	@Override
	public void push(Visitor v) {
		stack.add(0, v);
		this.top = v;
	}

	@Override
	public void result(Object r) {
		stack.remove(0);
		this.top = stack.get(0);
		if (this.top instanceof ResultAware)
			((ResultAware)this.top).result(r);
	}

	public void visitEntry(RepositoryEntry entry) {
		top.visitEntry(entry);
	}

	public void visitPrimitive(Primitive p) {
		top.visitPrimitive(p);
	}

	public void visitStructDefn(StructDefn s) {
		top.visitStructDefn(s);
	}

	public void visitStructField(StructField sf) {
		top.visitStructField(sf);
	}

	public void leaveStructDefn(StructDefn s) {
		top.leaveStructDefn(s);
	}

	public void visitUnresolvedVar(UnresolvedVar var, int nargs) {
		top.visitUnresolvedVar(var, nargs);
	}

	public void visitUnionTypeDefn(UnionTypeDefn ud) {
		top.visitUnionTypeDefn(ud);
	}

	public void leaveUnionTypeDefn(UnionTypeDefn ud) {
		top.leaveUnionTypeDefn(ud);
	}

	public void visitUnresolvedOperator(UnresolvedOperator operator, int nargs) {
		top.visitUnresolvedOperator(operator, nargs);
	}

	public void visitTypeReference(TypeReference var) {
		top.visitTypeReference(var);
	}

	public void visitFunction(FunctionDefinition fn) {
		top.visitFunction(fn);
	}

	public void visitFunctionIntro(FunctionIntro fi) {
		top.visitFunctionIntro(fi);
	}

	public void leaveFunctionIntro(FunctionIntro fi) {
		top.leaveFunctionIntro(fi);
	}

	public void leaveFunction(FunctionDefinition fn) {
		top.leaveFunction(fn);
	}

	public void visitPattern(Object patt) {
		top.visitPattern(patt);
	}

	public void visitVarPattern(VarPattern p) {
		top.visitVarPattern(p);
	}

	public void visitTypedPattern(TypedPattern p) {
		top.visitTypedPattern(p);
	}

	public void visitConstructorMatch(ConstructorMatch p) {
		top.visitConstructorMatch(p);
	}

	public void visitConstructorField(String field, Object patt) {
		top.visitConstructorField(field, patt);
	}

	public void leaveConstructorField(String field, Object patt) {
		top.leaveConstructorField(field, patt);
	}

	public void leaveConstructorMatch(ConstructorMatch p) {
		top.leaveConstructorMatch(p);
	}

	public void visitPatternVar(InputPosition varLoc, String var) {
		top.visitPatternVar(varLoc, var);
	}

	public void leavePattern(Object patt) {
		top.leavePattern(patt);
	}

	public void visitCase(FunctionCaseDefn c) {
		top.visitCase(c);
	}

	public void visitExpr(Expr expr, int nArgs) {
		top.visitExpr(expr, nArgs);
	}

	public void visitStringLiteral(StringLiteral expr) {
		top.visitStringLiteral(expr);
	}

	public void visitNumericLiteral(NumericLiteral number) {
		top.visitNumericLiteral(number);
	}

	public void visitUnitTestPackage(UnitTestPackage e) {
		top.visitUnitTestPackage(e);
	}

	public void visitUnitTest(UnitTestCase e) {
		top.visitUnitTest(e);
	}

	public void leaveUnitTest(UnitTestCase e) {
		top.leaveUnitTest(e);
	}

	public void leaveUnitTestPackage(UnitTestPackage e) {
		top.leaveUnitTestPackage(e);
	}

	public void visitApplyExpr(ApplyExpr expr) {
		top.visitApplyExpr(expr);
	}

	public void leaveApplyExpr(ApplyExpr expr) {
		top.leaveApplyExpr(expr);
	}

	public void visitUnitTestStep(UnitTestStep s) {
		top.visitUnitTestStep(s);
	}

	public void visitUnitTestAssert(UnitTestAssert a) {
		top.visitUnitTestAssert(a);
	}

	public void postUnitTestAssert(UnitTestAssert a) {
		top.postUnitTestAssert(a);
	}

	public void visitContractDecl(ContractDecl cd) {
		top.visitContractDecl(cd);
	}

	public void visitContractMethod(ContractMethodDecl cmd) {
		top.visitContractMethod(cmd);
	}

	public void leaveContractDecl(ContractDecl cd) {
		top.leaveContractDecl(cd);
	}

	public void visitObjectDefn(ObjectDefn e) {
		top.visitObjectDefn(e);
	}

	public void visitObjectMethod(ObjectMethod e) {
		top.visitObjectMethod(e);
	}
}
