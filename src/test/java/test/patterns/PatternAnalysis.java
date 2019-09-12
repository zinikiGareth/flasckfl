package test.patterns;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.UnitTestFileName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.parsedForm.ConstructorMatch;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.parser.ut.UnitTestNamer;
import org.flasck.flas.parser.ut.UnitTestPackageNamer;
import org.flasck.flas.patterns.HSIOptions;
import org.flasck.flas.patterns.PatternAnalyzer;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.Repository.Visitor;
import org.flasck.flas.repository.StackVisitor;
import org.flasck.flas.repository.Traverser;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

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
		assertEquals(0, fn.hsiTree().width());
	}
	
	@Test
	public void analyzeFunctionWithASingleVar() {
		FunctionDefinition fn = new FunctionDefinition(nameF, 1);
		final FunctionIntro intro;
		{
			ArrayList<Object> args = new ArrayList<>();
			args.add(new VarPattern(pos, new VarName(pos, nameF, "x")));
			intro = new FunctionIntro(nameF, args);
			intro.functionCase(new FunctionCaseDefn(null, number));
			fn.intro(intro);
		}
		new Traverser(sv).visitFunction(fn);
		assertNotNull(fn.hsiTree());
		assertEquals(1, fn.hsiTree().width());
		HSIOptions ha = fn.hsiTree().get(0);
		assertEquals(1, ha.vars().size());
		assertNotNull(intro.hsiTree());
	}
	
	@Test
	public void analyzeFunctionWithATypedVar() {
		FunctionDefinition fn = new FunctionDefinition(nameF, 1);
		final FunctionIntro intro;
		{
			ArrayList<Object> args = new ArrayList<>();
			TypeReference tr = new TypeReference(pos, "Number");
			tr.bind(LoadBuiltins.number);
			args.add(new TypedPattern(pos, tr, new VarName(pos, nameF, "x")));
			intro = new FunctionIntro(nameF, args);
			intro.functionCase(new FunctionCaseDefn(null, number));
			fn.intro(intro);
		}
		new Traverser(sv).visitFunction(fn);
		assertNotNull(fn.hsiTree());
		assertEquals(1, fn.hsiTree().width());
		HSIOptions ha = fn.hsiTree().get(0);
		assertEquals(1, ha.types().size());
		assertNotNull(intro.hsiTree());
	}
	
	@Test
	public void analyzeFunctionWithASimpleNoArgConstructor() {
		FunctionDefinition fn = new FunctionDefinition(nameF, 1);
		final FunctionIntro intro;
		{
			ArrayList<Object> args = new ArrayList<>();
			args.add(new ConstructorMatch(pos, "Nil"));
			intro = new FunctionIntro(nameF, args);
			intro.functionCase(new FunctionCaseDefn(null, number));
			fn.intro(intro);
		}
		new Traverser(sv).visitFunction(fn);
		assertNotNull(fn.hsiTree());
		assertEquals(1, fn.hsiTree().width());
		HSIOptions ha = fn.hsiTree().get(0);
		assertNotNull(ha.getCM("Nil"));
		assertNotNull(intro.hsiTree());
	}
}
