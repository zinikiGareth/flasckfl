package test.patterns;

import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.UnitTestFileName;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parser.ut.UnitTestNamer;
import org.flasck.flas.parser.ut.UnitTestPackageNamer;
import org.flasck.flas.patterns.PatternAnalyzer;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.Repository;
import org.flasck.flas.repository.Repository.Visitor;
import org.flasck.flas.repository.StackVisitor;
import org.flasck.flas.repository.Traverser;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import test.parsing.ExprMatcher;

public class PatternAnalysis {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private InputPosition pos = new InputPosition("-", 1, 0, "hello");
	private final PackageName pkg = new PackageName("test.repo");
	final StringLiteral simpleExpr = new StringLiteral(pos, "hello");
	final NumericLiteral number = new NumericLiteral(pos, 42);
	final UnitTestNamer namer = new UnitTestPackageNamer(new UnitTestFileName(pkg, "file"));
	final Visitor v = context.mock(Visitor.class);

	final FunctionName nameF = FunctionName.function(pos, pkg, "fred");
	final StackVisitor sv = new StackVisitor();
	final PatternAnalyzer analyzer = new PatternAnalyzer(null, null, sv);

//	@Before
//	public void initializeRepository() {
//		FunctionDefinition fn = new FunctionDefinition(nameF, 2);
//		final UnresolvedVar var = new UnresolvedVar(pos, "x");
//		{
//			final FunctionIntro intro = new FunctionIntro(nameF, new ArrayList<>());
//			intro.functionCase(new FunctionCaseDefn(null, var));
//			fn.intro(intro);
//		}
//		{
//			final FunctionIntro intro = new FunctionIntro(nameF, new ArrayList<>());
//			intro.functionCase(new FunctionCaseDefn(null, new ApplyExpr(pos, var, number)));
//			fn.intro(intro);
//		}
//	}
	
	@Test
	public void analyzeFunctionWithNoArguments() {
		FunctionDefinition fn = new FunctionDefinition(nameF, 0);
		{
			final FunctionIntro intro = new FunctionIntro(nameF, new ArrayList<>());
			intro.functionCase(new FunctionCaseDefn(null, number));
			fn.intro(intro);
		}
		new Traverser(sv).visitFunction(fn);
		assertNotNull(fn.hsiTree());
	}
}
