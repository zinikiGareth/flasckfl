package test.repository;

import java.util.ArrayList;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.UnitTestFileName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.hsi.TreeOrderVisitor;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.parser.ut.UnitTestNamer;
import org.flasck.flas.parser.ut.UnitTestPackageNamer;
import org.flasck.flas.patterns.HSIArgsTree;
import org.flasck.flas.patterns.HSICtorTree;
import org.flasck.flas.patterns.HSITree;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.Repository;
import org.flasck.flas.repository.Traverser;
import org.jmock.Expectations;
import org.jmock.Sequence;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class TreeOrderTraversalTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private InputPosition pos = new InputPosition("-", 1, 0, "hello");
	private final PackageName pkg = new PackageName("test.repo");
	final FunctionName nameF = FunctionName.function(pos, pkg, "f");
	final StringLiteral simpleExpr = new StringLiteral(pos, "hello");
	final NumericLiteral number = new NumericLiteral(pos, "42", 2);
	final UnresolvedVar var = new UnresolvedVar(pos, "f");
	final UnresolvedOperator op = new UnresolvedOperator(pos, "+");
	final UnitTestNamer namer = new UnitTestPackageNamer(new UnitTestFileName(pkg, "file"));
	final Repository r = new Repository();
	final TreeOrderVisitor v = context.mock(TreeOrderVisitor.class);
	// TODO: the function groups here is not interesting - but it can be applied on a case-by-case basis and is really just an excuse to write this comment to remind me to write some tests like that later ...
	final Traverser t = new Traverser(v).withNestedPatterns().withFunctionsInDependencyGroups(null).withPatternsInTreeOrder();;

	@Test
	public void aVeryDullFunctionHasAlmostNothingToDo() {
		FunctionDefinition fn = new FunctionDefinition(nameF, 0);
		FunctionIntro fi = new FunctionIntro(nameF, new ArrayList<>());
		fn.intro(fi);
		fn.bindHsi(new HSIArgsTree(0));
		Sequence seq = context.sequence("order");
		context.checking(new Expectations() {{
			oneOf(v).visitFunction(fn); inSequence(seq);
			oneOf(v).visitFunctionIntro(fi); inSequence(seq);
			oneOf(v).leaveFunctionIntro(fi); inSequence(seq);
			oneOf(v).leaveFunction(fn); inSequence(seq);
		}});
		t.visitFunction(fn);
	}

	@Test
	public void aFunctionWithTwoIntrosVisitsAllCasesEvenWithoutPatterns() {
		FunctionDefinition fn = new FunctionDefinition(nameF, 0);
		FunctionIntro fi1 = new FunctionIntro(nameF, new ArrayList<>());
		fi1.functionCase(new FunctionCaseDefn(null, simpleExpr));
		fn.intro(fi1);
		FunctionIntro fi2 = new FunctionIntro(nameF, new ArrayList<>());
		fi2.functionCase(new FunctionCaseDefn(null, number));
		fn.intro(fi2);
		fn.bindHsi(new HSIArgsTree(0));
		Sequence seq = context.sequence("order");
		context.checking(new Expectations() {{
			oneOf(v).visitFunction(fn); inSequence(seq);
			oneOf(v).visitFunctionIntro(fi1); inSequence(seq);
			oneOf(v).visitExpr(simpleExpr, 0); inSequence(seq);
			oneOf(v).visitStringLiteral(simpleExpr); inSequence(seq);
			oneOf(v).leaveFunctionIntro(fi1); inSequence(seq);
			oneOf(v).visitFunctionIntro(fi2); inSequence(seq);
			oneOf(v).visitExpr(number, 0); inSequence(seq);
			oneOf(v).visitNumericLiteral(number); inSequence(seq);
			oneOf(v).leaveFunctionIntro(fi2); inSequence(seq);
			oneOf(v).leaveFunction(fn); inSequence(seq);
		}});
		t.visitFunction(fn);
	}

	@Test
	public void aFunctionWithTwoIntrosAndOneArgVisitsThePatternsFirstThenTheCases() {
		FunctionDefinition fn = new FunctionDefinition(nameF, 1);
		FunctionIntro fi1 = new FunctionIntro(nameF, new ArrayList<>()); // Note this should have a pattern, but that duplicates creating the hsitree
		fi1.functionCase(new FunctionCaseDefn(null, simpleExpr));
		fn.intro(fi1);
		FunctionIntro fi2 = new FunctionIntro(nameF, new ArrayList<>());
		fi2.functionCase(new FunctionCaseDefn(null, number));
		fn.intro(fi2);
		HSITree hsi = new HSIArgsTree(1);
		hsi.get(0).requireCM(LoadBuiltins.nil).consider(fi1);
		hsi.get(0).requireCM(LoadBuiltins.cons).consider(fi2);
		fn.bindHsi(hsi);
		Sequence seq = context.sequence("order");
		context.checking(new Expectations() {{
			oneOf(v).visitFunction(fn); inSequence(seq);
			oneOf(v).argSlot(with(SlotMatcher.id("0"))); inSequence(seq);
			oneOf(v).matchConstructor(with(LoadBuiltins.cons)); inSequence(seq);
			oneOf(v).matchConstructor(with(LoadBuiltins.nil)); inSequence(seq);
			oneOf(v).visitFunctionIntro(fi1); inSequence(seq);
			oneOf(v).visitExpr(simpleExpr, 0); inSequence(seq);
			oneOf(v).visitStringLiteral(simpleExpr); inSequence(seq);
			oneOf(v).leaveFunctionIntro(fi1); inSequence(seq);
			oneOf(v).visitFunctionIntro(fi2); inSequence(seq);
			oneOf(v).visitExpr(number, 0); inSequence(seq);
			oneOf(v).visitNumericLiteral(number); inSequence(seq);
			oneOf(v).leaveFunctionIntro(fi2); inSequence(seq);
			oneOf(v).leaveFunction(fn); inSequence(seq);
		}});
		t.visitFunction(fn);
	}

	@Test
	public void multipleArgumentsCanBeHandledInTurn() {
		FunctionDefinition fn = new FunctionDefinition(nameF, 2);
		FunctionIntro fi = new FunctionIntro(nameF, new ArrayList<>()); // Note this should have a pattern, but that duplicates creating the hsitree
		fi.functionCase(new FunctionCaseDefn(null, simpleExpr));
		fn.intro(fi);
		HSITree hsi = new HSIArgsTree(2);
		hsi.get(0).requireCM(LoadBuiltins.cons).consider(fi);
		hsi.get(1).requireCM(LoadBuiltins.nil).consider(fi);
		fn.bindHsi(hsi);
		Sequence seq = context.sequence("order");
		context.checking(new Expectations() {{
			oneOf(v).visitFunction(fn); inSequence(seq);
			oneOf(v).argSlot(with(SlotMatcher.id("0"))); inSequence(seq);
			oneOf(v).matchConstructor(with(LoadBuiltins.cons)); inSequence(seq);
			oneOf(v).argSlot(with(SlotMatcher.id("1"))); inSequence(seq);
			oneOf(v).matchConstructor(with(LoadBuiltins.nil)); inSequence(seq);
			oneOf(v).visitFunctionIntro(fi); inSequence(seq);
			oneOf(v).visitExpr(simpleExpr, 0); inSequence(seq);
			oneOf(v).visitStringLiteral(simpleExpr); inSequence(seq);
			oneOf(v).leaveFunctionIntro(fi); inSequence(seq);
			oneOf(v).leaveFunction(fn); inSequence(seq);
		}});
		t.visitFunction(fn);
	}

	@Test
	public void structsCanMatchFields() {
		FunctionDefinition fn = new FunctionDefinition(nameF, 2);
		FunctionIntro fi = new FunctionIntro(nameF, new ArrayList<>()); // Note this should have a pattern, but that duplicates creating the hsitree
		fi.functionCase(new FunctionCaseDefn(null, simpleExpr));
		fn.intro(fi);
		HSITree hsi = new HSIArgsTree(2);
		HSICtorTree cons = hsi.get(0).requireCM(LoadBuiltins.cons);
		cons.consider(fi);
		VarName vn = new VarName(pos, nameF, "h");
		VarPattern vp = new VarPattern(pos, vn);
		cons.field("head").addVar(vp, fi);
		cons.field("tail").requireCM(LoadBuiltins.nil);
		hsi.get(1).requireCM(LoadBuiltins.nil).consider(fi);
		fn.bindHsi(hsi);
		Sequence seq = context.sequence("order");
		context.checking(new Expectations() {{
			oneOf(v).visitFunction(fn); inSequence(seq);
			oneOf(v).argSlot(with(SlotMatcher.id("0"))); inSequence(seq);
			oneOf(v).matchConstructor(with(LoadBuiltins.cons)); inSequence(seq);
			oneOf(v).matchField(LoadBuiltins.cons.findField("head")); inSequence(seq);
			oneOf(v).varInIntro(vp, fi); inSequence(seq);
			oneOf(v).matchField(LoadBuiltins.cons.findField("tail")); inSequence(seq);
			oneOf(v).matchConstructor(with(LoadBuiltins.nil)); inSequence(seq);
			oneOf(v).argSlot(with(SlotMatcher.id("1"))); inSequence(seq);
			oneOf(v).matchConstructor(with(LoadBuiltins.nil)); inSequence(seq);
			oneOf(v).visitFunctionIntro(fi); inSequence(seq);
			oneOf(v).visitExpr(simpleExpr, 0); inSequence(seq);
			oneOf(v).visitStringLiteral(simpleExpr); inSequence(seq);
			oneOf(v).leaveFunctionIntro(fi); inSequence(seq);
			oneOf(v).leaveFunction(fn); inSequence(seq);
		}});
		t.visitFunction(fn);
	}
	
	@Test
	public void typesAreOKToo() {
		FunctionDefinition fn = new FunctionDefinition(nameF, 2);
		FunctionIntro fi = new FunctionIntro(nameF, new ArrayList<>()); // Note this should have a pattern, but that duplicates creating the hsitree
		fi.functionCase(new FunctionCaseDefn(null, simpleExpr));
		fn.intro(fi);
		HSITree hsi = new HSIArgsTree(1);
		VarName vx = new VarName(pos, nameF, "x");
		hsi.get(0).addTyped(new TypedPattern(pos, new TypeReference(pos, "Number").bind(LoadBuiltins.number), vx), fi);
		fn.bindHsi(hsi);
		Sequence seq = context.sequence("order");
		context.checking(new Expectations() {{
			oneOf(v).visitFunction(fn); inSequence(seq);
			oneOf(v).argSlot(with(SlotMatcher.id("0"))); inSequence(seq);
			oneOf(v).matchType(LoadBuiltins.number, vx, fi); inSequence(seq);
			oneOf(v).visitFunctionIntro(fi); inSequence(seq);
			oneOf(v).visitExpr(simpleExpr, 0); inSequence(seq);
			oneOf(v).visitStringLiteral(simpleExpr); inSequence(seq);
			oneOf(v).leaveFunctionIntro(fi); inSequence(seq);
			oneOf(v).leaveFunction(fn); inSequence(seq);
		}});
		t.visitFunction(fn);
	}

	@Test
	public void aFunctionWithTwoIntrosGivesEachOneTheRightVariableArguments() {
		FunctionDefinition fn = new FunctionDefinition(nameF, 2);
		FunctionIntro fi1 = new FunctionIntro(nameF, new ArrayList<>()); // Note this should have a pattern, but that duplicates creating the hsitree
		fi1.functionCase(new FunctionCaseDefn(null, simpleExpr));
		fn.intro(fi1);
		FunctionIntro fi2 = new FunctionIntro(nameF, new ArrayList<>());
		fi2.functionCase(new FunctionCaseDefn(null, number));
		fn.intro(fi2);
		VarName vn = new VarName(pos, nameF, "v");
		VarPattern vp = new VarPattern(pos, vn);
		VarName xn = new VarName(pos, nameF, "x");
		VarPattern xp = new VarPattern(pos, xn);
		HSITree hsi = new HSIArgsTree(2);
		VarName nn = new VarName(pos, nameF, "n");
		hsi.get(0).addTyped(new TypedPattern(pos, new TypeReference(pos, "Number").bind(LoadBuiltins.number), nn), fi1);
		VarName sn = new VarName(pos, nameF, "s");
		hsi.get(0).addTyped(new TypedPattern(pos, new TypeReference(pos, "String").bind(LoadBuiltins.string), sn), fi2);
		hsi.get(1).addVar(vp, fi1);
		hsi.get(1).addVar(xp, fi2);
		fn.bindHsi(hsi);
		Sequence seq = context.sequence("order");
		context.checking(new Expectations() {{
			oneOf(v).visitFunction(fn); inSequence(seq);
			oneOf(v).argSlot(with(SlotMatcher.id("0"))); inSequence(seq);
			oneOf(v).matchType(LoadBuiltins.number, nn, fi1); inSequence(seq);
			oneOf(v).matchType(LoadBuiltins.string, sn, fi2); inSequence(seq);
			oneOf(v).argSlot(with(SlotMatcher.id("1"))); inSequence(seq);
			oneOf(v).varInIntro(vp, fi1); inSequence(seq);
			oneOf(v).varInIntro(xp, fi2); inSequence(seq);
			oneOf(v).visitFunctionIntro(fi1); inSequence(seq);
			oneOf(v).visitExpr(simpleExpr, 0); inSequence(seq);
			oneOf(v).visitStringLiteral(simpleExpr); inSequence(seq);
			oneOf(v).leaveFunctionIntro(fi1); inSequence(seq);
			oneOf(v).visitFunctionIntro(fi2); inSequence(seq);
			oneOf(v).visitExpr(number, 0); inSequence(seq);
			oneOf(v).visitNumericLiteral(number); inSequence(seq);
			oneOf(v).leaveFunctionIntro(fi2); inSequence(seq);
			oneOf(v).leaveFunction(fn); inSequence(seq);
		}});
		t.visitFunction(fn);
	}
}