package test.repository;

import java.util.ArrayList;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.UnitTestFileName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.hsi.ArgSlot;
import org.flasck.flas.hsi.TreeOrderVisitor;
import org.flasck.flas.lifting.DependencyGroup;
import org.flasck.flas.lifting.FunctionGroupOrdering;
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
import org.flasck.flas.repository.FunctionGroup;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.Repository;
import org.flasck.flas.repository.Traverser;
import org.flasck.flas.testsupport.matchers.SlotMatcher;
import org.jmock.Expectations;
import org.jmock.Sequence;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class TreeOrderTraversalTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private InputPosition pos = new InputPosition("-", 1, 0, null, "hello");
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
	final Traverser t = new Traverser(v).withNestedPatterns().withFunctionsInDependencyGroups(new FunctionGroupOrdering(new ArrayList<FunctionGroup>()));
	private final FunctionIntro intro = null;

	@Test
	public void aVeryDullFunctionHasAlmostNothingToDo() {
		FunctionDefinition fn = new FunctionDefinition(nameF, 0, null);
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
		FunctionDefinition fn = new FunctionDefinition(nameF, 0, null);
		FunctionIntro fi1 = new FunctionIntro(nameF, new ArrayList<>());
		FunctionCaseDefn fcd1 = new FunctionCaseDefn(pos, intro, null, simpleExpr);
		fi1.functionCase(fcd1);
		fn.intro(fi1);
		FunctionIntro fi2 = new FunctionIntro(nameF, new ArrayList<>());
		FunctionCaseDefn fcd2 = new FunctionCaseDefn(pos, intro, null, number);
		fi2.functionCase(fcd2);
		fn.intro(fi2);
		fn.bindHsi(new HSIArgsTree(0));
		Sequence seq = context.sequence("order");
		context.checking(new Expectations() {{
			oneOf(v).visitFunction(fn); inSequence(seq);
			oneOf(v).visitFunctionIntro(fi1); inSequence(seq);
			oneOf(v).visitCase(fcd1);
			oneOf(v).leaveCase(fcd1);
			oneOf(v).visitExpr(simpleExpr, 0); inSequence(seq);
			oneOf(v).visitStringLiteral(simpleExpr); inSequence(seq);
			oneOf(v).leaveFunctionIntro(fi1); inSequence(seq);
			oneOf(v).visitFunctionIntro(fi2); inSequence(seq);
			oneOf(v).visitCase(fcd2);
			oneOf(v).leaveCase(fcd2);
			oneOf(v).visitExpr(number, 0); inSequence(seq);
			oneOf(v).visitNumericLiteral(number); inSequence(seq);
			oneOf(v).leaveFunctionIntro(fi2); inSequence(seq);
			oneOf(v).leaveFunction(fn); inSequence(seq);
		}});
		t.visitFunction(fn);
	}

	@Test
	public void aFunctionWithTwoIntrosAndOneArgVisitsThePatternsFirstThenTheCases() {
		FunctionDefinition fn = new FunctionDefinition(nameF, 1, null);
		FunctionIntro fi1 = new FunctionIntro(nameF, new ArrayList<>()); // Note this should have a pattern, but that duplicates creating the hsitree
		FunctionCaseDefn fcd1 = new FunctionCaseDefn(pos, intro, null, simpleExpr);
		fi1.functionCase(fcd1);
		fn.intro(fi1);
		FunctionIntro fi2 = new FunctionIntro(nameF, new ArrayList<>());
		FunctionCaseDefn fcd2 = new FunctionCaseDefn(pos, intro, null, number);
		fi2.functionCase(fcd2);
		fn.intro(fi2);
		HSITree hsi = new HSIArgsTree(1);
		hsi.get(0).requireCM(LoadBuiltins.nil).consider(fi1);
		hsi.get(0).requireCM(LoadBuiltins.cons).consider(fi2);
		fn.bindHsi(hsi);
		Sequence seq = context.sequence("order");
		DependencyGroup grp = new DependencyGroup(fn);
		context.checking(new Expectations() {{
			oneOf(v).visitFunctionGroup(grp);
			oneOf(v).visitFunction(fn); inSequence(seq);
			oneOf(v).argSlot((ArgSlot) with(SlotMatcher.id("0"))); inSequence(seq);
			oneOf(v).matchConstructor(with(LoadBuiltins.cons)); inSequence(seq);
			oneOf(v).endConstructor(with(LoadBuiltins.cons)); inSequence(seq);
			oneOf(v).matchConstructor(with(LoadBuiltins.nil)); inSequence(seq);
			oneOf(v).endConstructor(with(LoadBuiltins.nil)); inSequence(seq);
			oneOf(v).endArg(with(SlotMatcher.id("0"))); inSequence(seq);
			oneOf(v).patternsDone(fn); inSequence(seq);
			oneOf(v).leaveFunction(fn); inSequence(seq);
			oneOf(v).visitFunction(fn); inSequence(seq);
			oneOf(v).visitFunctionIntro(fi1); inSequence(seq);
			oneOf(v).visitCase(fcd1); inSequence(seq);
			oneOf(v).visitExpr(simpleExpr, 0); inSequence(seq);
			oneOf(v).visitStringLiteral(simpleExpr); inSequence(seq);
			oneOf(v).leaveCase(fcd1); inSequence(seq);
			oneOf(v).leaveFunctionIntro(fi1); inSequence(seq);
			oneOf(v).visitFunctionIntro(fi2); inSequence(seq);
			oneOf(v).visitCase(fcd2); inSequence(seq);
			oneOf(v).visitExpr(number, 0); inSequence(seq);
			oneOf(v).visitNumericLiteral(number); inSequence(seq);
			oneOf(v).leaveCase(fcd2); inSequence(seq);
			oneOf(v).leaveFunctionIntro(fi2); inSequence(seq);
			oneOf(v).leaveFunction(fn); inSequence(seq);
			oneOf(v).leaveFunctionGroup(grp);
		}});
		t.visitFunctionGroup(grp);
	}

	@Test
	public void multipleArgumentsCanBeHandledInTurn() {
		FunctionDefinition fn = new FunctionDefinition(nameF, 2, null);
		FunctionIntro fi = new FunctionIntro(nameF, new ArrayList<>()); // Note this should have a pattern, but that duplicates creating the hsitree
		FunctionCaseDefn fcd = new FunctionCaseDefn(pos, intro, null, simpleExpr);
		fi.functionCase(fcd);
		fn.intro(fi);
		HSITree hsi = new HSIArgsTree(2);
		hsi.get(0).requireCM(LoadBuiltins.cons).consider(fi);
		hsi.get(1).requireCM(LoadBuiltins.nil).consider(fi);
		fn.bindHsi(hsi);
		Sequence seq = context.sequence("order");
		DependencyGroup grp = new DependencyGroup(fn);
		context.checking(new Expectations() {{
			oneOf(v).visitFunctionGroup(grp);
			oneOf(v).visitFunction(fn); inSequence(seq);
			oneOf(v).argSlot((ArgSlot) with(SlotMatcher.id("0"))); inSequence(seq);
			oneOf(v).matchConstructor(with(LoadBuiltins.cons)); inSequence(seq);
			oneOf(v).endConstructor(with(LoadBuiltins.cons)); inSequence(seq);
			oneOf(v).endArg(with(SlotMatcher.id("0"))); inSequence(seq);
			oneOf(v).argSlot((ArgSlot) with(SlotMatcher.id("1"))); inSequence(seq);
			oneOf(v).matchConstructor(with(LoadBuiltins.nil)); inSequence(seq);
			oneOf(v).endConstructor(with(LoadBuiltins.nil)); inSequence(seq);
			oneOf(v).endArg(with(SlotMatcher.id("1"))); inSequence(seq);
			oneOf(v).patternsDone(fn); inSequence(seq);
			oneOf(v).leaveFunction(fn); inSequence(seq);
			oneOf(v).visitFunction(fn); inSequence(seq);
			oneOf(v).visitFunctionIntro(fi); inSequence(seq);
			oneOf(v).visitCase(fcd); inSequence(seq);
			oneOf(v).visitExpr(simpleExpr, 0); inSequence(seq);
			oneOf(v).visitStringLiteral(simpleExpr); inSequence(seq);
			oneOf(v).leaveCase(fcd); inSequence(seq);
			oneOf(v).leaveFunctionIntro(fi); inSequence(seq);
			oneOf(v).leaveFunction(fn); inSequence(seq);
			oneOf(v).leaveFunctionGroup(grp);
		}});
		t.visitFunctionGroup(grp);
	}

	@Test
	public void structsCanMatchFields() {
		FunctionDefinition fn = new FunctionDefinition(nameF, 2, null);
		FunctionIntro fi = new FunctionIntro(nameF, new ArrayList<>()); // Note this should have a pattern, but that duplicates creating the hsitree
		FunctionCaseDefn fcd = new FunctionCaseDefn(pos, intro, null, simpleExpr);
		fi.functionCase(fcd);
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
		DependencyGroup grp = new DependencyGroup(fn);
		context.checking(new Expectations() {{
			oneOf(v).visitFunctionGroup(grp);
			oneOf(v).visitFunction(fn); inSequence(seq);
			oneOf(v).argSlot((ArgSlot) with(SlotMatcher.id("0"))); inSequence(seq);
			oneOf(v).matchConstructor(with(LoadBuiltins.cons)); inSequence(seq);
			oneOf(v).matchField(LoadBuiltins.cons.findField("head")); inSequence(seq);
			oneOf(v).varInIntro(vn, vp, fi); inSequence(seq);
			oneOf(v).endField(LoadBuiltins.cons.findField("head")); inSequence(seq);
			oneOf(v).matchField(LoadBuiltins.cons.findField("tail")); inSequence(seq);
			oneOf(v).matchConstructor(with(LoadBuiltins.nil)); inSequence(seq);
			oneOf(v).endConstructor(with(LoadBuiltins.nil)); inSequence(seq);
			oneOf(v).endField(LoadBuiltins.cons.findField("tail")); inSequence(seq);
			oneOf(v).endConstructor(with(LoadBuiltins.cons)); inSequence(seq);
			oneOf(v).endArg(with(SlotMatcher.id("0"))); inSequence(seq);
			oneOf(v).argSlot((ArgSlot) with(SlotMatcher.id("1"))); inSequence(seq);
			oneOf(v).matchConstructor(with(LoadBuiltins.nil)); inSequence(seq);
			oneOf(v).endConstructor(with(LoadBuiltins.nil)); inSequence(seq);
			oneOf(v).endArg(with(SlotMatcher.id("1"))); inSequence(seq);
			oneOf(v).patternsDone(fn); inSequence(seq);
			oneOf(v).leaveFunction(fn); inSequence(seq);
			oneOf(v).visitFunction(fn); inSequence(seq);
			oneOf(v).visitFunctionIntro(fi); inSequence(seq);
			oneOf(v).visitCase(fcd); inSequence(seq);
			oneOf(v).visitExpr(simpleExpr, 0); inSequence(seq);
			oneOf(v).visitStringLiteral(simpleExpr); inSequence(seq);
			oneOf(v).leaveCase(fcd); inSequence(seq);
			oneOf(v).leaveFunctionIntro(fi); inSequence(seq);
			oneOf(v).leaveFunction(fn); inSequence(seq);
			oneOf(v).leaveFunctionGroup(grp);
		}});
		t.visitFunctionGroup(grp);
	}
	
	@Test
	public void typesAreOKToo() {
		FunctionDefinition fn = new FunctionDefinition(nameF, 2, null);
		FunctionIntro fi = new FunctionIntro(nameF, new ArrayList<>()); // Note this should have a pattern, but that duplicates creating the hsitree
		FunctionCaseDefn fcd = new FunctionCaseDefn(pos, intro, null, simpleExpr);
		fi.functionCase(fcd);
		fn.intro(fi);
		HSITree hsi = new HSIArgsTree(1);
		VarName vx = new VarName(pos, nameF, "x");
		hsi.get(0).addTyped(new TypedPattern(pos, new TypeReference(pos, "Number").bind(LoadBuiltins.number), vx), fi);
		fn.bindHsi(hsi);
		Sequence seq = context.sequence("order");
		DependencyGroup grp = new DependencyGroup(fn);
		context.checking(new Expectations() {{
			oneOf(v).visitFunctionGroup(grp);
			oneOf(v).visitFunction(fn); inSequence(seq);
			oneOf(v).argSlot((ArgSlot) with(SlotMatcher.id("0"))); inSequence(seq);
			oneOf(v).matchType(LoadBuiltins.number, vx, fi); inSequence(seq);
			oneOf(v).endArg(with(SlotMatcher.id("0"))); inSequence(seq);
			oneOf(v).patternsDone(fn); inSequence(seq);
			oneOf(v).leaveFunction(fn); inSequence(seq);
			oneOf(v).visitFunction(fn); inSequence(seq);
			oneOf(v).visitFunctionIntro(fi); inSequence(seq);
			oneOf(v).visitCase(fcd); inSequence(seq);
			oneOf(v).visitExpr(simpleExpr, 0); inSequence(seq);
			oneOf(v).visitStringLiteral(simpleExpr); inSequence(seq);
			oneOf(v).leaveCase(fcd); inSequence(seq);
			oneOf(v).leaveFunctionIntro(fi); inSequence(seq);
			oneOf(v).leaveFunction(fn); inSequence(seq);
			oneOf(v).leaveFunctionGroup(grp);
		}});
		t.visitFunctionGroup(grp);
	}

	@Test
	public void aFunctionWithTwoIntrosGivesEachOneTheRightVariableArguments() {
		FunctionDefinition fn = new FunctionDefinition(nameF, 2, null);
		FunctionIntro fi1 = new FunctionIntro(nameF, new ArrayList<>()); // Note this should have a pattern, but that duplicates creating the hsitree
		FunctionCaseDefn fcd1 = new FunctionCaseDefn(pos, intro, null, simpleExpr);
		fi1.functionCase(fcd1);
		fn.intro(fi1);
		FunctionIntro fi2 = new FunctionIntro(nameF, new ArrayList<>());
		FunctionCaseDefn fcd2 = new FunctionCaseDefn(pos, intro, null, number);
		fi2.functionCase(fcd2);
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
		DependencyGroup grp = new DependencyGroup(fn);
		context.checking(new Expectations() {{
			oneOf(v).visitFunctionGroup(grp);
			oneOf(v).visitFunction(fn); inSequence(seq);
			oneOf(v).argSlot((ArgSlot) with(SlotMatcher.id("0"))); inSequence(seq);
			oneOf(v).matchType(LoadBuiltins.number, nn, fi1); inSequence(seq);
			oneOf(v).matchType(LoadBuiltins.string, sn, fi2); inSequence(seq);
			oneOf(v).endArg(with(SlotMatcher.id("0"))); inSequence(seq);
			oneOf(v).argSlot((ArgSlot) with(SlotMatcher.id("1"))); inSequence(seq);
			oneOf(v).varInIntro(vn, vp, fi1); inSequence(seq);
			oneOf(v).varInIntro(xn, xp, fi2); inSequence(seq);
			oneOf(v).endArg(with(SlotMatcher.id("1"))); inSequence(seq);
			oneOf(v).patternsDone(fn); inSequence(seq);
			oneOf(v).leaveFunction(fn); inSequence(seq);
			oneOf(v).visitFunction(fn); inSequence(seq);
			oneOf(v).visitFunctionIntro(fi1); inSequence(seq);
			oneOf(v).visitCase(fcd1); inSequence(seq);
			oneOf(v).visitExpr(simpleExpr, 0); inSequence(seq);
			oneOf(v).visitStringLiteral(simpleExpr); inSequence(seq);
			oneOf(v).leaveCase(fcd1); inSequence(seq);
			oneOf(v).leaveFunctionIntro(fi1); inSequence(seq);
			oneOf(v).visitFunctionIntro(fi2); inSequence(seq);
			oneOf(v).visitCase(fcd2); inSequence(seq);
			oneOf(v).visitExpr(number, 0); inSequence(seq);
			oneOf(v).visitNumericLiteral(number); inSequence(seq);
			oneOf(v).leaveCase(fcd2); inSequence(seq);
			oneOf(v).leaveFunctionIntro(fi2); inSequence(seq);
			oneOf(v).leaveFunction(fn); inSequence(seq);
			oneOf(v).leaveFunctionGroup(grp);
		}});
		t.visitFunctionGroup(grp);
	}
}
