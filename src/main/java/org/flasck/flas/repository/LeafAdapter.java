package org.flasck.flas.repository;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.ConstPattern;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.parsedForm.ActionMessage;
import org.flasck.flas.parsedForm.AssignMessage;
import org.flasck.flas.parsedForm.ConstructorMatch;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.MakeSend;
import org.flasck.flas.parsedForm.Messages;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.SendMessage;
import org.flasck.flas.parsedForm.StandaloneMethod;
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
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.parser.ut.UnitDataDeclaration.Assignment;
import org.flasck.flas.repository.Repository.Visitor;
import org.flasck.flas.tc3.Primitive;

/** The purpose of the leaf adapter is to visit all of the leaves in the system.
 *
 *  This is not suitable for things that want to see (for example) 'apply' or 'dot' nodes in expressions, which should use the Pre or Post adapters 
 */
public class LeafAdapter implements Visitor {
	@Override
	public void visitEntry(RepositoryEntry entry) {
	}

	@Override
	public void visitPrimitive(Primitive p) {
	}

	@Override
	public void visitStructDefn(StructDefn s) {
	}

	@Override
	public void leaveStructDefn(StructDefn s) {
	}

	@Override
	public void visitStructField(StructField sf) {
	}

	@Override
	public void visitUnionTypeDefn(UnionTypeDefn ud) {
	}

	@Override
	public void leaveUnionTypeDefn(UnionTypeDefn ud) {
	}

	@Override
	public void visitObjectDefn(ObjectDefn obj) {
	}

	@Override
	public void leaveObjectDefn(ObjectDefn obj) {
	}

	@Override
	public void visitObjectMethod(ObjectMethod meth) {
	}

	@Override
	public void visitStandaloneMethod(StandaloneMethod meth) {
	}

	@Override
	public void visitMessage(ActionMessage msg) {
	}

	@Override
	public void visitAssignMessage(AssignMessage msg) {
	}

	@Override
	public void visitAssignSlot(List<UnresolvedVar> slot) {
	}

	@Override
	public void leaveAssignMessage(AssignMessage msg) {
	}

	@Override
	public void visitSendMessage(SendMessage msg) {
	}

	@Override
	public void leaveSendMessage(SendMessage msg) {
	}

	@Override
	public void visitMessages(Messages messages) {
	}

	@Override
	public void leaveMessages(Messages msgs) {
	}

	@Override
	public void leaveMessage(ActionMessage msg) {
	}

	@Override
	public void leaveObjectMethod(ObjectMethod meth) {
	}

	@Override
	public void leaveStandaloneMethod(StandaloneMethod meth) {
	}

	@Override
	public void visitFunctionGroup(FunctionGroup grp) {
	}
	
	@Override
	public void visitFunction(FunctionDefinition fn) {
	}

	@Override
	public void visitFunctionIntro(FunctionIntro fi) {
	}

	@Override
	public void leaveFunctionIntro(FunctionIntro fi) {
	}

	@Override
	public void leaveFunction(FunctionDefinition fn) {
	}

	@Override
	public void leaveFunctionGroup(FunctionGroup grp) {
	}

	@Override
	public void visitPattern(Pattern patt, boolean isNested) {
	}

	@Override
	public void visitVarPattern(VarPattern p, boolean isNested) {
	}

	@Override
	public void visitTypedPattern(TypedPattern p, boolean isNested) {
	}

	@Override
	public void visitConstructorMatch(ConstructorMatch p, boolean isNested) {
	}

	@Override
	public void visitConstructorField(String field, Pattern patt, boolean isNested) {
	}

	@Override
	public void leaveConstructorField(String field, Object patt) {
	}

	@Override
	public void leaveConstructorMatch(ConstructorMatch p) {
	}

	@Override
	public void visitConstPattern(ConstPattern p, boolean isNested) {
	}

	@Override
	public void visitPatternVar(InputPosition varLoc, String var) {
	}

	@Override
	public void leavePattern(Object patt, boolean isNested) {
	}

	@Override
	public void visitCase(FunctionCaseDefn c) {
	}

	@Override
	public void startInline(FunctionIntro fi) {
	}

	@Override
	public void visitGuard(FunctionCaseDefn c) {
	}

	@Override
	public void leaveGuard(FunctionCaseDefn c) {
	}

	@Override
	public void leaveCase(FunctionCaseDefn c) {
	}

	@Override
	public void endInline(FunctionIntro fi) {
	}

	@Override
	public void visitExpr(Expr expr, int nArgs) {
	}

	@Override
	public void visitApplyExpr(ApplyExpr expr) {
	}

	@Override
	public void leaveApplyExpr(ApplyExpr expr) {
	}

	@Override
	public void visitMemberExpr(MemberExpr expr) {
	}

	@Override
	public void leaveMemberExpr(MemberExpr expr) {
	}

	@Override
	public void visitStringLiteral(StringLiteral expr) {
	}

	@Override
	public void visitNumericLiteral(NumericLiteral number) {
	}

	@Override
	public void visitUnresolvedVar(UnresolvedVar var, int nargs) {
	}

	@Override
	public void visitUnresolvedOperator(UnresolvedOperator operator, int nargs) {
	}

	@Override
	public void visitTypeReference(TypeReference var) {
	}

	@Override
	public void visitMakeSend(MakeSend expr) {
	}

	@Override
	public void visitUnitTestPackage(UnitTestPackage e) {
	}

	@Override
	public void visitUnitTest(UnitTestCase e) {
	}

	@Override
	public void leaveUnitTest(UnitTestCase e) {
	}

	@Override
	public void leaveUnitTestPackage(UnitTestPackage e) {
	}
	
	@Override
	public void visitUnitDataDeclaration(UnitDataDeclaration udd) {
	}

	@Override
	public void leaveUnitDataDeclaration(UnitDataDeclaration udd) {
	}

	@Override
	public void visitUnitDataField(Assignment assign) {
	}

	@Override
	public void leaveUnitDataField(Assignment assign) {
	}

	@Override
	public void visitUnitTestAssert(UnitTestAssert a) {
	}

	@Override
	public void visitUnitTestStep(UnitTestStep s) {
	}

	@Override
	public void visitAssertExpr(boolean isValue, Expr e) {
	}

	@Override
	public void leaveAssertExpr(boolean isValue, Expr e) {
	}

	@Override
	public void postUnitTestAssert(UnitTestAssert a) {
	}

	@Override
	public void visitContractDecl(ContractDecl cd) {
	}

	@Override
	public void visitContractMethod(ContractMethodDecl cmd) {
	}

	@Override
	public void leaveContractMethod(ContractMethodDecl cmd) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void leaveContractDecl(ContractDecl cd) {
	}
}
