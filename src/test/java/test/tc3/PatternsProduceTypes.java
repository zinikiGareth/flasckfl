package test.tc3;

import java.util.ArrayList;
import java.util.Arrays;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.hsi.ArgSlot;
import org.flasck.flas.hsi.Slot;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.tc3.CurrentTCState;
import org.flasck.flas.tc3.ExpressionChecker;
import org.flasck.flas.tc3.ExpressionChecker.ExprResult;
import org.flasck.flas.tc3.FunctionChecker;
import org.flasck.flas.tc3.FunctionChecker.ArgResult;
import org.flasck.flas.tc3.SlotChecker;
import org.hamcrest.Matchers;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import flas.matchers.ApplyMatcher;

// These are not really "unit" types because they are covering too much ground
// To do what they purport to do, you need to cut into the pattern analyzer and see the type constraints that come out using visitInTheTCWay ...
public class PatternsProduceTypes {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private InputPosition pos = new InputPosition("-", 1, 0, "hello");
	private final PackageName pkg = new PackageName("test.repo");
	private final ErrorReporter errors = context.mock(ErrorReporter.class);
	private final NestedVisitor sv = context.mock(NestedVisitor.class);
	private final CurrentTCState state = context.mock(CurrentTCState.class);
	final FunctionName nameF = FunctionName.function(pos, pkg, "fred");
	
	@SuppressWarnings("unchecked")
	@Test
	public void aConstantPatternIsANumber() {
		FunctionChecker fc = new FunctionChecker(errors, sv, state);
		FunctionDefinition fn = new FunctionDefinition(nameF, 1);
		FunctionIntro fi = new FunctionIntro(nameF, new ArrayList<>());
		fn.intro(fi);
		fc.visitFunction(fn);
		context.checking(new Expectations() {{
			oneOf(state).createUT(null);
			oneOf(sv).push(with(any(SlotChecker.class)));
		}});
		Slot s = new ArgSlot(0, null);
		fc.argSlot(s);
		fc.result(new ArgResult(LoadBuiltins.number));
		context.checking(new Expectations() {{
			oneOf(sv).push(with(any(ExpressionChecker.class)));
		}});
		fc.visitFunctionIntro(fi);
		fc.visitCase(null);
		context.assertIsSatisfied();
		fc.result(new ExprResult(pos, LoadBuiltins.number));
		fc.leaveFunctionIntro(fi);
		context.checking(new Expectations() {{
			oneOf(state).consolidate(pos, Arrays.asList(LoadBuiltins.number)); will(returnValue(LoadBuiltins.number));
			oneOf(sv).result(with(ApplyMatcher.type(Matchers.is(LoadBuiltins.number), Matchers.is(LoadBuiltins.number))));
		}});
		fc.leaveFunction(fn);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void aStringConstantPatternIsAString() {
		FunctionChecker fc = new FunctionChecker(errors, sv, state);
		FunctionDefinition fn = new FunctionDefinition(nameF, 1);
		FunctionIntro fi = new FunctionIntro(nameF, new ArrayList<>());
		fn.intro(fi);
		fc.visitFunction(fn);
		context.checking(new Expectations() {{
			oneOf(state).createUT(null);
			oneOf(sv).push(with(any(SlotChecker.class)));
		}});
		Slot s = new ArgSlot(0, null);
		fc.argSlot(s);
		fc.result(new ArgResult(LoadBuiltins.string));
		context.checking(new Expectations() {{
			oneOf(sv).push(with(any(ExpressionChecker.class)));
		}});
		fc.visitFunctionIntro(fi);
		fc.visitCase(null);
		fc.result(new ExprResult(pos, LoadBuiltins.number));
		fc.leaveFunctionIntro(fi);
		context.checking(new Expectations() {{
			oneOf(state).consolidate(pos, Arrays.asList(LoadBuiltins.number)); will(returnValue(LoadBuiltins.number));
			oneOf(sv).result(with(ApplyMatcher.type(Matchers.is(LoadBuiltins.string), Matchers.is(LoadBuiltins.number))));
		}});
		fc.leaveFunction(fn);
	}
}
