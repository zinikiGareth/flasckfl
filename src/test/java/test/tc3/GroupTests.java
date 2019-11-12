package test.tc3;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.hsi.ArgSlot;
import org.flasck.flas.lifting.DependencyGroup;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.patterns.HSIOptions;
import org.flasck.flas.patterns.HSIPatternOptions;
import org.flasck.flas.repository.FunctionGroup;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.tc3.CurrentTCState;
import org.flasck.flas.tc3.ExpressionChecker;
import org.flasck.flas.tc3.ExpressionChecker.ExprResult;
import org.flasck.flas.tc3.FunctionChecker;
import org.flasck.flas.tc3.FunctionChecker.ArgResult;
import org.flasck.flas.tc3.FunctionGroupTCState;
import org.flasck.flas.tc3.GroupChecker;
import org.flasck.flas.tc3.SlotChecker;
import org.flasck.flas.tc3.Type;
import org.flasck.flas.tc3.TypeConstraintSet;
import org.flasck.flas.tc3.UnifiableType;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.support.jmock.CaptureAction;

public class GroupTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private final ErrorReporter errors = context.mock(ErrorReporter.class);
	private final RepositoryReader repository = context.mock(RepositoryReader.class);
	private final NestedVisitor sv = context.mock(NestedVisitor.class);
	private InputPosition pos = new InputPosition("-", 1, 0, "hello");
	private final PackageName pkg = new PackageName("test.repo");
	final FunctionName nameF = FunctionName.function(pos, pkg, "f");
	FunctionDefinition fnF = new FunctionDefinition(nameF, 1);
	final FunctionName nameG = FunctionName.function(pos, pkg, "g");
	FunctionDefinition fnG = new FunctionDefinition(nameG, 1);
	List<Pattern> args = new ArrayList<>();
	FunctionIntro fiF1 = new FunctionIntro(nameF, args);
	FunctionIntro fiF2 = new FunctionIntro(nameF, args);
	FunctionIntro fiG1 = new FunctionIntro(nameG, args);
	FunctionIntro fiG2 = new FunctionIntro(nameG, args);
	private FunctionGroup grp = new DependencyGroup(fnF, fnG);
	private CurrentTCState state = new FunctionGroupTCState(repository, grp);
	private final GroupChecker gc = new GroupChecker(errors, repository, sv, state);

	@Before
	public void begin() {
		context.checking(new Expectations() {{
			fnF.intro(fiF1);
			fnF.intro(fiF2);
			fnG.intro(fiG1);
			fnG.intro(fiG2);
		}});
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void mutuallyRecursiveFunctionsAreAllDecidedAtTheEnd() {
		// I want to write a test that says that f and g are mutually recursive
		// I think I want to conclude that f :: Number->String and g :: String->String
		// But obviously in between I need to know that f is UT1 and is (UT2 String) returns String
		//                                          and g is UT2 and is (UT1 Number) returns String
		// What we actually know is that they both have 1 arg and that that is typed pattern (Number/String)
		// And then we have two possible return types, one of which is the "UT application" and the other is the type we want
		// Unification is then reasonable.
		
		// What I want to *prove* is that we don't resolve too soon.
		
		UnifiableType utF = new TypeConstraintSet(repository, state, pos, "utf");
		UnifiableType utG = new TypeConstraintSet(repository, state, pos, "utg");
		
		CaptureAction captureFCF = new CaptureAction(null);
		context.checking(new Expectations() {{
			oneOf(sv).push(with(any(FunctionChecker.class))); will(captureFCF);
		}});
		gc.visitFunction(fnF);
		context.assertIsSatisfied();
		FunctionChecker fcf = (FunctionChecker)captureFCF.get(0);

		context.checking(new Expectations() {{
			oneOf(sv).push(with(any(SlotChecker.class)));
		}});
		fcf.argSlot(new ArgSlot(0, null));
		fcf.result(new ArgResult(LoadBuiltins.number));

		context.checking(new Expectations() {{
			oneOf(sv).push(with(any(ExpressionChecker.class)));
		}});
		fcf.visitFunctionIntro(fiF1);
		fcf.visitCase(null);
		UnifiableType r1 = utG.canBeAppliedTo(Arrays.asList(LoadBuiltins.number));
		fcf.result(new ExprResult(r1));
		fcf.leaveFunctionIntro(fiF1);

		context.checking(new Expectations() {{
			oneOf(sv).push(with(any(ExpressionChecker.class)));
		}});
		fcf.visitFunctionIntro(fiF2);
		fcf.visitCase(null);
		fcf.result(new ExprResult(LoadBuiltins.string));
		fcf.leaveFunctionIntro(fiF2);
		
		CaptureAction captureFType = new CaptureAction(null);
		context.checking(new Expectations() {{
			oneOf(sv).result(with(ApplyMatcher.type(Matchers.is(LoadBuiltins.number), 
					(Matcher)ConsolidatedTypeMatcher.with(
							Matchers.is(LoadBuiltins.string)
					)
			))); will(captureFType);
		}});
		fcf.leaveFunction(fnF);
		context.assertIsSatisfied();
		gc.result(captureFType.get(0));
		
		context.assertIsSatisfied();
		CaptureAction captureFCG = new CaptureAction(null);
		context.checking(new Expectations() {{
			oneOf(sv).push(with(any(FunctionChecker.class))); will(captureFCG);
		}});
		gc.visitFunction(fnG);
		context.assertIsSatisfied();
		FunctionChecker fcg = (FunctionChecker)captureFCG.get(0);

		context.checking(new Expectations() {{
			oneOf(sv).push(with(any(SlotChecker.class)));
		}});
		fcg.argSlot(new ArgSlot(0, null));
		fcg.result(new ArgResult(LoadBuiltins.string));

		context.checking(new Expectations() {{
			oneOf(sv).push(with(any(ExpressionChecker.class)));
		}});
		fcg.visitFunctionIntro(fiG1);
		fcg.visitCase(null);
		UnifiableType r2 = utF.canBeAppliedTo(Arrays.asList(LoadBuiltins.string));
		fcg.result(new ExprResult(r2));
		fcg.leaveFunctionIntro(fiG1);

		context.checking(new Expectations() {{
			oneOf(sv).push(with(any(ExpressionChecker.class)));
		}});
		fcg.visitFunctionIntro(fiG1);
		fcg.visitCase(null);
		fcg.result(new ExprResult(LoadBuiltins.string));
		fcg.leaveFunctionIntro(fiG1);

		CaptureAction captureGType = new CaptureAction(null);
		context.checking(new Expectations() {{
			oneOf(sv).result(with(ApplyMatcher.type(Matchers.is(LoadBuiltins.string), 
					(Matcher)ConsolidatedTypeMatcher.with(
							Matchers.is(LoadBuiltins.string)
					)
			))); will(captureGType);
		}});
		fcg.leaveFunction(fnG);
		context.assertIsSatisfied();
		gc.result(captureGType.get(0));
		
		context.checking(new Expectations() {{
			// the "any" here is because we haven't constrained the result type since returning it
			oneOf(repository).findUnionWith((Set)with(Matchers.contains(Matchers.is(LoadBuiltins.string)))); will(returnValue(LoadBuiltins.string));
			oneOf(repository).findUnionWith((Set)with(Matchers.contains(Matchers.is(LoadBuiltins.string)))); will(returnValue(LoadBuiltins.string));
			oneOf(sv).result(null); // leave function group doesn't propagate anything ...
		}});
		gc.leaveFunctionGroup(grp);
		assertNotNull(fnF.type());
		assertThat(fnF.type(), (Matcher)ApplyMatcher.type(Matchers.is(LoadBuiltins.number), Matchers.is(LoadBuiltins.string)));
		assertNotNull(fnG.type());
		assertThat(fnG.type(), (Matcher)ApplyMatcher.type(Matchers.is(LoadBuiltins.string), Matchers.is(LoadBuiltins.string)));
	}

	// TODO: I want to add another test where the return value of f/g is used (eg by +) so that we deduce its type and add constraints in a different way.

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void aVarPatternHasItsTypeBoundEvenIfItsAFunction() {
		UnifiableType fnArg = state.createUT();
		
		CaptureAction captureFCF = new CaptureAction(null);
		context.checking(new Expectations() {{
			oneOf(sv).push(with(any(FunctionChecker.class))); will(captureFCF);
		}});
		gc.visitFunction(fnF);
		context.assertIsSatisfied();
		FunctionChecker fcf = (FunctionChecker)captureFCF.get(0);

		CaptureAction captureSC = new CaptureAction(null);
		context.checking(new Expectations() {{
			oneOf(sv).push(with(any(SlotChecker.class))); will(captureSC);
		}});
		HSIOptions opts = new HSIPatternOptions();
		VarPattern pattG = new VarPattern(pos, new VarName(pos, nameF, "g"));
		opts.addVar(pattG, fiF2);
		fcf.argSlot(new ArgSlot(0, opts));
		context.assertIsSatisfied();
		((SlotChecker)captureSC.get(0)).varInIntro(pattG.name(), pattG, fiF2);
		
		fcf.result(new ArgResult(fnArg));

		context.checking(new Expectations() {{
			oneOf(sv).push(with(any(ExpressionChecker.class)));
		}});
		fcf.visitFunctionIntro(fiF2);
		fcf.visitCase(null);
		UnifiableType r1 = fnArg.canBeAppliedTo(Arrays.asList(LoadBuiltins.number));
		r1.canBeType(LoadBuiltins.string);
		fcf.result(new ExprResult(r1));
		fcf.leaveFunctionIntro(fiF2);
		
		CaptureAction captureFType = new CaptureAction(null);
		context.checking(new Expectations() {{
			oneOf(sv).result(with(ApplyMatcher.type(Matchers.is(fnArg), 
							Matchers.is(r1)
			))); will(captureFType);
		}});
		fcf.leaveFunction(fnF);
		context.assertIsSatisfied();
		gc.result(captureFType.get(0));

		context.checking(new Expectations() {{
			oneOf(sv).result(null); // leave function group doesn't propagate anything ...
		}});
		gc.leaveFunctionGroup(grp);
		assertNotNull(fnF.type());
		assertThat(fnF.type(), (Matcher)ApplyMatcher.type((Matcher)ApplyMatcher.type(Matchers.is(LoadBuiltins.number), Matchers.is(LoadBuiltins.string)), Matchers.is(LoadBuiltins.string)));
		Type argType = fnArg.resolve();
		assertNotNull(argType);
		assertThat(argType, (Matcher)ApplyMatcher.type(Matchers.is(LoadBuiltins.number), Matchers.is(LoadBuiltins.string)));
		assertNotNull(pattG.type());
	}
}
