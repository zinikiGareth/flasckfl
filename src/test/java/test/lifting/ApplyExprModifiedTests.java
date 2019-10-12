package test.lifting;

import java.util.ArrayList;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.hsi.HSIVisitor;
import org.flasck.flas.lifting.MappingStore;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.repository.Traverser;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import test.parsing.ExprMatcher;

public class ApplyExprModifiedTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	InputPosition pos = new InputPosition("-", 1, 0, null);
	PackageName pkg = new PackageName("test.foo");
	HSIVisitor hsi = context.mock(HSIVisitor.class);

	@Test
	public void aCallToANestedFunctionWithVarsGetsThosePassedIn() {
		FunctionName nameF = FunctionName.function(pos, pkg, "f");
		VarPattern vp = new VarPattern(pos, new VarName(pos, nameF, "x"));
//		FunctionDefinition ff = new FunctionDefinition(nameF, 1);
		
		FunctionName nameG = FunctionName.function(pos, nameF, "g");
		FunctionDefinition fn = new FunctionDefinition(nameG, 1);
		ArrayList<Object> args = new ArrayList<>();
		args.add(new TypedPattern(pos, new TypeReference(pos, "String"), new VarName(pos, nameG, "x")));
		FunctionIntro fi = new FunctionIntro(nameG, args);
		fn.intro(fi);

		MappingStore ms = new MappingStore();
		ms.recordNestedVar(fi, vp);
//		ms.recordDependency(ff, fn);
		fn.nestedVars(ms);

		UnresolvedVar fnCall = new UnresolvedVar(pos, "g");
		fnCall.bind(fn);
		StringLiteral sl = new StringLiteral(pos, "hello");
		ApplyExpr ae = new ApplyExpr(pos, fnCall, sl);
		
		context.checking(new Expectations() {{
			oneOf(hsi).visitApplyExpr((ApplyExpr) with(ExprMatcher.apply(ExprMatcher.unresolved("g"), ExprMatcher.unresolved("x"), ExprMatcher.string(sl.text))));
			oneOf(hsi).visitExpr(fnCall, 2);
			oneOf(hsi).visitUnresolvedVar(fnCall, 2);
			oneOf(hsi).visitExpr(with(ExprMatcher.unresolved("x")), with(0));
			oneOf(hsi).visitUnresolvedVar((UnresolvedVar) with(ExprMatcher.unresolved("x")), with(0));
			oneOf(hsi).visitExpr(sl, 0);
			oneOf(hsi).visitStringLiteral(sl);
			oneOf(hsi).leaveApplyExpr((ApplyExpr) with(ExprMatcher.apply(ExprMatcher.unresolved("g"), ExprMatcher.unresolved("x"), ExprMatcher.string(sl.text))));
		}});
		Traverser traverser = new Traverser(hsi).withNestedPatterns();
//		traverser.rememberCaller(ff);
		traverser.visitApplyExpr(ae);
	}
	
	@Test
	public void aCallToANestedFunctionWhichIsJustAVarBecomesAnAEWithNestedVars() {
		FunctionName nameF = FunctionName.function(pos, pkg, "f");
		VarPattern vp = new VarPattern(pos, new VarName(pos, nameF, "x"));
		
		FunctionName nameG = FunctionName.function(pos, nameF, "g");
		FunctionDefinition fn = new FunctionDefinition(nameG, 0);
		FunctionIntro fi = new FunctionIntro(nameG, new ArrayList<>());
		fn.intro(fi);

		MappingStore ms = new MappingStore();
		ms.recordNestedVar(fi, vp);
		fn.nestedVars(ms);

		UnresolvedVar fnCall = new UnresolvedVar(pos, "g");
		fnCall.bind(fn);
		
		context.checking(new Expectations() {{
			oneOf(hsi).visitApplyExpr((ApplyExpr) with(ExprMatcher.apply(ExprMatcher.unresolved("g"), ExprMatcher.unresolved("x"))));
			oneOf(hsi).visitExpr(fnCall, 1);
			oneOf(hsi).visitUnresolvedVar(fnCall, 1);
			oneOf(hsi).visitExpr(with(ExprMatcher.unresolved("x")), with(0));
			oneOf(hsi).visitUnresolvedVar((UnresolvedVar) with(ExprMatcher.unresolved("x")), with(0));
			oneOf(hsi).leaveApplyExpr((ApplyExpr) with(ExprMatcher.apply(ExprMatcher.unresolved("g"), ExprMatcher.unresolved("x"))));
		}});
		Traverser traverser = new Traverser(hsi).withNestedPatterns();
		traverser.visitUnresolvedVar(fnCall, 0);
	}
	
}
