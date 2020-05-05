package test.tc3;

import java.util.ArrayList;
import java.util.Arrays;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.hsi.ArgSlot;
import org.flasck.flas.hsi.Slot;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.tc3.CurrentTCState;
import org.flasck.flas.tc3.ExpressionChecker;
import org.flasck.flas.tc3.ExpressionChecker.ExprResult;
import org.flasck.flas.tc3.ExpressionChecker.GuardResult;
import org.flasck.flas.tc3.FunctionChecker;
import org.flasck.flas.tc3.FunctionChecker.ArgResult;
import org.flasck.flas.tc3.SlotChecker;
import org.flasck.flas.tc3.UnifiableType;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.support.jmock.CaptureAction;

import flas.matchers.ApplyMatcher;
import flas.matchers.PosMatcher;

// These are not really "unit" types because they are covering too much ground
// To do what they purport to do, you need to cut into the pattern analyzer and see the type constraints that come out using visitInTheTCWay ...
public class GuardTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private InputPosition pos = new InputPosition("-", 1, 0, "hello");
	private final PackageName pkg = new PackageName("test.repo");
	private final ErrorReporter errors = context.mock(ErrorReporter.class);
	private final NestedVisitor sv = context.mock(NestedVisitor.class);
	private final CurrentTCState state = context.mock(CurrentTCState.class);
	final FunctionName nameF = FunctionName.function(pos, pkg, "fred");
	private RepositoryReader repository = context.mock(RepositoryReader.class);

	@Before
	public void allowFC() {
		context.checking(new Expectations() {{
			oneOf(sv).push(with(any(FunctionChecker.class)));
		}});
	}
	
	// Assume we have something like
	//  | (2 == 3) = x
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void weCanHandleASimpleCorrectCase() {
		FunctionChecker fc = new FunctionChecker(errors, repository, sv, state, null);
		FunctionDefinition fn = new FunctionDefinition(nameF, 1, false);
		FunctionIntro fi = new FunctionIntro(nameF, new ArrayList<>());
		fn.intro(fi);
		Expr test = new UnresolvedVar(pos, "True");
		Expr res = new UnresolvedVar(pos, "x");
		FunctionCaseDefn fic1 = new FunctionCaseDefn(test, res);
		fc.visitFunction(fn);
		context.checking(new Expectations() {{
			oneOf(state).createUT(null, "slot ArgSlot[0]");
			oneOf(sv).push(with(any(SlotChecker.class)));
		}});
		Slot s = new ArgSlot(0, null);
		fc.argSlot(s);
		fc.result(new ArgResult(LoadBuiltins.number));
		CaptureAction eg = new CaptureAction(null);
		context.checking(new Expectations() {{
			oneOf(sv).push(with(any(ExpressionChecker.class))); will(eg);
		}});
		fc.visitFunctionIntro(fi);
		fc.visitCase(fic1);

		context.assertIsSatisfied();
		ExpressionChecker ec = (ExpressionChecker) eg.get(0);

		ec.visitGuard(fic1);
		CaptureAction gr = new CaptureAction(null);
		context.checking(new Expectations() {{
			oneOf(sv).result(with(any(GuardResult.class))); will(gr);
		}});
		ec.result(LoadBuiltins.bool);

		context.assertIsSatisfied();
		CaptureAction eg2 = new CaptureAction(null);

		context.checking(new Expectations() {{
			oneOf(sv).push(with(any(ExpressionChecker.class))); will(eg2);
		}});
		fc.result(gr.get(0));

		context.assertIsSatisfied();
		ExpressionChecker ec2 = (ExpressionChecker) eg2.get(0);

		CaptureAction er = new CaptureAction(null);
		context.checking(new Expectations() {{
			oneOf(sv).result(with(any(ExprResult.class))); will(er);
		}});
		ec2.result(LoadBuiltins.number);

		context.assertIsSatisfied();
		fc.result(er.get(0));
		
		fc.leaveCase(fic1);
		fc.leaveFunctionIntro(fi);
		context.checking(new Expectations() {{
			oneOf(state).consolidate(pos, Arrays.asList(new ExprResult(pos, LoadBuiltins.number))); will(returnValue(new ExprResult(pos, LoadBuiltins.number)));
			oneOf(sv).result(with(PosMatcher.type((Matcher)ApplyMatcher.type(Matchers.is(LoadBuiltins.number), Matchers.is(LoadBuiltins.number)))));
		}});
		fc.leaveFunction(fn);
	}

	@Test
	public void theGuardTypeCanBeTrue() {
		FunctionChecker fc = new FunctionChecker(errors, repository, sv, state, null);
		FunctionDefinition fn = new FunctionDefinition(nameF, 1, false);
		FunctionIntro fi = new FunctionIntro(nameF, new ArrayList<>());
		fn.intro(fi);
		fc.visitFunction(fn);

		context.checking(new Expectations() {{
			oneOf(sv).push(with(any(ExpressionChecker.class)));
		}});
		fc.result(new GuardResult(pos, LoadBuiltins.trueT));
	}

	@Test
	public void theGuardTypeCanBeFalse() {
		FunctionChecker fc = new FunctionChecker(errors, repository, sv, state, null);
		FunctionDefinition fn = new FunctionDefinition(nameF, 1, false);
		FunctionIntro fi = new FunctionIntro(nameF, new ArrayList<>());
		fn.intro(fi);
		fc.visitFunction(fn);

		context.checking(new Expectations() {{
			oneOf(sv).push(with(any(ExpressionChecker.class)));
		}});
		fc.result(new GuardResult(pos, LoadBuiltins.falseT));
	}

	@Test
	public void theGuardTypeCanBeAUTForNowButWeDemandItIsBoolean() {
		FunctionChecker fc = new FunctionChecker(errors, repository, sv, state, null);
		FunctionDefinition fn = new FunctionDefinition(nameF, 1, false);
		FunctionIntro fi = new FunctionIntro(nameF, new ArrayList<>());
		fn.intro(fi);
		fc.visitFunction(fn);

		UnifiableType ut = context.mock(UnifiableType.class);
		context.checking(new Expectations() {{
			oneOf(ut).incorporatedBy(pos, LoadBuiltins.bool);
			oneOf(sv).push(with(any(ExpressionChecker.class)));
		}});
		fc.result(new GuardResult(pos, ut));
	}

	@Test
	public void itsAnErrorForTheGuardTypeToBeNumber() {
		FunctionChecker fc = new FunctionChecker(errors, repository, sv, state, null);
		FunctionDefinition fn = new FunctionDefinition(nameF, 1, false);
		FunctionIntro fi = new FunctionIntro(nameF, new ArrayList<>());
		fn.intro(fi);
		fc.visitFunction(fn);

		context.checking(new Expectations() {{
			oneOf(errors).message(pos, "guards must be booleans");
			oneOf(sv).push(with(any(ExpressionChecker.class)));
		}});
		fc.result(new GuardResult(pos, LoadBuiltins.number));
	}
}
