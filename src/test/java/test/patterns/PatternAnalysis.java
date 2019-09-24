package test.patterns;

import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.UnitTestFileName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.hsi.ArgSlot;
import org.flasck.flas.hsi.HSIVisitor;
import org.flasck.flas.hsi.Slot;
import org.flasck.flas.parsedForm.ConstructorMatch;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.parser.ut.UnitTestNamer;
import org.flasck.flas.parser.ut.UnitTestPackageNamer;
import org.flasck.flas.patterns.PatternAnalyzer;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.Repository.Visitor;
import org.flasck.flas.repository.StackVisitor;
import org.flasck.flas.repository.Traverser;
import org.jmock.Expectations;
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

	@Test
	public void analyzeFunctionWithNoArguments() {
		FunctionDefinition fn = new FunctionDefinition(nameF, 0);
		final FunctionIntro intro;
		{
			intro = new FunctionIntro(nameF, new ArrayList<>());
			intro.functionCase(new FunctionCaseDefn(null, number));
			fn.intro(intro);
		}
		new Traverser(sv).visitFunction(fn);
		HSIVisitor hsi = context.mock(HSIVisitor.class);
		ArrayList<Slot> slots = new ArrayList<>();
		context.checking(new Expectations() {{
			oneOf(hsi).startInline(intro);
			oneOf(hsi).visitExpr(number, 0);
			oneOf(hsi).visitNumericLiteral(number);
			oneOf(hsi).endInline(intro);
		}});
		fn.hsiTree().visit(new Traverser(hsi), hsi, slots);
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
		HSIVisitor hsi = context.mock(HSIVisitor.class);
		ArrayList<Slot> slots = new ArrayList<>();
		ArgSlot s0 = new ArgSlot(0);
		slots.add(s0);
		context.checking(new Expectations() {{
			oneOf(hsi).bind(s0, "x");
			oneOf(hsi).startInline(intro);
			oneOf(hsi).visitExpr(number, 0);
			oneOf(hsi).visitNumericLiteral(number);
			oneOf(hsi).endInline(intro);
		}});
		fn.hsiTree().visit(new Traverser(hsi), hsi, slots);
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
		HSIVisitor hsi = context.mock(HSIVisitor.class);
		ArrayList<Slot> slots = new ArrayList<>();
		slots.add(new ArgSlot(0));
		context.checking(new Expectations() {{
			oneOf(hsi).startInline(intro);
			oneOf(hsi).visitExpr(number, 0);
			oneOf(hsi).visitNumericLiteral(number);
			oneOf(hsi).endInline(intro);
		}});
		fn.hsiTree().visit(new Traverser(hsi), hsi, slots);
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
		HSIVisitor hsi = context.mock(HSIVisitor.class);
		ArrayList<Slot> slots = new ArrayList<>();
		ArgSlot a0 = new ArgSlot(0);
		slots.add(a0);
		context.checking(new Expectations() {{
			oneOf(hsi).switchOn(a0);
			oneOf(hsi).withConstructor("Nil");
			oneOf(hsi).startInline(intro);
			oneOf(hsi).visitExpr(number, 0);
			oneOf(hsi).visitNumericLiteral(number);
			oneOf(hsi).endInline(intro);
			oneOf(hsi).errorNoCase();
			oneOf(hsi).endSwitch();
		}});
		fn.hsiTree().visit(new Traverser(hsi), hsi, slots);
	}
	
	@Test
	public void analyzeFunctionWithAChoiceOfTwoConstructors() {
		FunctionDefinition fn = new FunctionDefinition(nameF, 1);
		final FunctionIntro intro1;
		{
			ArrayList<Object> args = new ArrayList<>();
			args.add(new ConstructorMatch(pos, "Nil"));
			intro1 = new FunctionIntro(nameF, args);
			intro1.functionCase(new FunctionCaseDefn(null, number));
			fn.intro(intro1);
		}
		final FunctionIntro intro2;
		{
			ArrayList<Object> args = new ArrayList<>();
			args.add(new ConstructorMatch(pos, "Cons"));
			intro2 = new FunctionIntro(nameF, args);
			intro2.functionCase(new FunctionCaseDefn(null, simpleExpr));
			fn.intro(intro2);
		}
		new Traverser(sv).visitFunction(fn);
		HSIVisitor hsi = context.mock(HSIVisitor.class);
		ArrayList<Slot> slots = new ArrayList<>();
		ArgSlot a0 = new ArgSlot(0);
		slots.add(a0);
		context.checking(new Expectations() {{
			oneOf(hsi).switchOn(a0);
			oneOf(hsi).withConstructor("Nil");
			oneOf(hsi).startInline(intro1);
			oneOf(hsi).visitExpr(number, 0);
			oneOf(hsi).visitNumericLiteral(number);
			oneOf(hsi).endInline(intro1);
			oneOf(hsi).withConstructor("Cons");
			oneOf(hsi).startInline(intro2);
			oneOf(hsi).visitExpr(simpleExpr, 0);
			oneOf(hsi).visitStringLiteral(simpleExpr);
			oneOf(hsi).endInline(intro2);
			oneOf(hsi).errorNoCase();
			oneOf(hsi).endSwitch();
		}});
		fn.hsiTree().visit(new Traverser(hsi), hsi, slots);
		assertNotNull(fn.hsiTree());
	}
}
