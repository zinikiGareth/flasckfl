package test.repository;

import java.util.ArrayList;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.UnitTestFileName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parser.ut.UnitTestNamer;
import org.flasck.flas.parser.ut.UnitTestPackageNamer;
import org.flasck.flas.repository.Repository;
import org.flasck.flas.repository.RepositoryVisitor;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import flas.matchers.ExprMatcher;

public class FunctionTraversalTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private InputPosition pos = new InputPosition("-", 1, 0, null, "hello");
	private final PackageName pkg = new PackageName("test.repo");
	final StringLiteral simpleExpr = new StringLiteral(pos, "hello");
	final NumericLiteral number = new NumericLiteral(pos, 42);
	final UnitTestNamer namer = new UnitTestPackageNamer(new UnitTestFileName(pkg, "file"));
	final Repository r = new Repository();
	final RepositoryVisitor v = context.mock(RepositoryVisitor.class);
	final UnresolvedVar var = new UnresolvedVar(pos, "x");
	private final FunctionIntro intro = null;
	final FunctionCaseDefn fcd1 = new FunctionCaseDefn(pos, intro, null, var);
	final FunctionCaseDefn fcd2 = new FunctionCaseDefn(pos, intro, null, new ApplyExpr(pos, var, number));
	final ErrorReporter errors = context.mock(ErrorReporter.class);

	@Before
	public void initializeRepository() {
		final FunctionName nameF = FunctionName.function(pos, pkg, "fred");
		FunctionDefinition fn = new FunctionDefinition(nameF, 2, null);
		{
			final FunctionIntro intro = new FunctionIntro(nameF, new ArrayList<>());
			intro.functionCase(fcd1);
			fn.intro(intro);
		}
		{
			final FunctionIntro intro = new FunctionIntro(nameF, new ArrayList<>());
			intro.functionCase(fcd2);
			fn.intro(intro);
		}
		r.functionDefn(errors, fn);
	}
	
	@Test
	public void handleTraversal() {
		context.checking(new Expectations() {{
			ExprMatcher ae = ExprMatcher.apply(ExprMatcher.unresolved("x"), ExprMatcher.number(42));
			oneOf(v).visitFunction(with(any(FunctionDefinition.class)));
			oneOf(v).visitFunctionIntro(with(any(FunctionIntro.class)));
			oneOf(v).visitCase(fcd1);
			oneOf(v).leaveCase(fcd1);
			oneOf(v).visitExpr((UnresolvedVar) with(ExprMatcher.unresolved("x")), with(0));
			oneOf(v).visitUnresolvedVar((UnresolvedVar) with(ExprMatcher.unresolved("x")), with(0));
			oneOf(v).leaveFunctionIntro(with(any(FunctionIntro.class)));
			oneOf(v).visitFunctionIntro(with(any(FunctionIntro.class)));
			oneOf(v).visitCase(fcd2);
			oneOf(v).leaveCase(fcd2);
			oneOf(v).visitExpr((ApplyExpr) with(ae), with(0));
			oneOf(v).visitApplyExpr((ApplyExpr) with(ae));
			oneOf(v).visitExpr((UnresolvedVar) with(ExprMatcher.unresolved("x")), with(1));
			oneOf(v).visitUnresolvedVar((UnresolvedVar) with(ExprMatcher.unresolved("x")), with(1));
			oneOf(v).visitExpr(number, 0);
			oneOf(v).visitNumericLiteral(number);
			oneOf(v).leaveApplyExpr((ApplyExpr) with(ae));
			oneOf(v).leaveFunctionIntro(with(any(FunctionIntro.class)));
			oneOf(v).leaveFunction(with(any(FunctionDefinition.class)));
			oneOf(v).traversalDone();
		}});
		r.traverse(v);
	}
}
