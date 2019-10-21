package test.tc3;

import java.util.ArrayList;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ConstPattern;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.hsi.ArgSlot;
import org.flasck.flas.hsi.Slot;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.patterns.HSIArgsTree;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryReader;
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

// These are not really "unit" types because they are covering too much ground
// To do what they purport to do, you need to cut into the pattern analyzer and see the type constraints that come out using visitInTheTCWay ...
public class PatternsProduceTypes {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private InputPosition pos = new InputPosition("-", 1, 0, "hello");
	private final PackageName pkg = new PackageName("test.repo");
	private final ErrorReporter errors = context.mock(ErrorReporter.class);
	private final RepositoryReader repository = context.mock(RepositoryReader.class);
	private final NestedVisitor sv = context.mock(NestedVisitor.class);
	private final CurrentTCState state = context.mock(CurrentTCState.class);
	final FunctionName nameF = FunctionName.function(pos, pkg, "fred");
	
	@Before
	public void begin() {
		context.checking(new Expectations() {{
			allowing(state);
		}});
	}

	@SuppressWarnings("unchecked")
	@Test
	public void aConstantPatternIsANumber() {
		FunctionChecker fc = new FunctionChecker(errors, repository, sv, state);
		FunctionDefinition fn = new FunctionDefinition(nameF, 1);
		ArrayList<Object> args = new ArrayList<>();
		args.add(new ConstPattern(pos, ConstPattern.INTEGER, "42"));
		FunctionIntro fi = new FunctionIntro(nameF, args);
		HSIArgsTree hat = new HSIArgsTree(1);
		hat.consider(fi);
		hat.get(0).addConstant(LoadBuiltins.number, "42", fi);
		fi.bindTree(hat);
		fn.intro(fi);
		fn.bindHsi(hat);
		fc.visitFunction(fn);
		context.checking(new Expectations() {{
			oneOf(sv).push(with(any(SlotChecker.class)));
		}});
		Slot s = new ArgSlot(0, hat.get(0));
		fc.argSlot(s);
		fc.result(new ArgResult(LoadBuiltins.number));
		context.checking(new Expectations() {{
			oneOf(sv).push(with(any(ExpressionChecker.class)));
		}});
		fc.visitFunctionIntro(fi);
		context.assertIsSatisfied();
		fc.result(new ExprResult(LoadBuiltins.number));
		fc.leaveFunctionIntro(fi);
		context.checking(new Expectations() {{
			oneOf(sv).result(with(ApplyMatcher.type(Matchers.is(LoadBuiltins.number), Matchers.is(LoadBuiltins.number))));
		}});
		fc.leaveFunction(fn);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void aStringConstantPatternIsAString() {
		FunctionChecker fc = new FunctionChecker(errors, repository, sv, state);
		FunctionDefinition fn = new FunctionDefinition(nameF, 1);
		ArrayList<Object> args = new ArrayList<>();
		args.add(new ConstPattern(pos, ConstPattern.INTEGER, "42"));
		FunctionIntro fi = new FunctionIntro(nameF, args);
		HSIArgsTree hat = new HSIArgsTree(1);
		hat.consider(fi);
		hat.get(0).addConstant(LoadBuiltins.string, "42", fi);
		fi.bindTree(hat);
		fn.intro(fi);
		fn.bindHsi(hat);
		fc.visitFunction(fn);
		context.checking(new Expectations() {{
			oneOf(sv).push(with(any(SlotChecker.class)));
		}});
		Slot s = new ArgSlot(0, hat.get(0));
		fc.argSlot(s);
		fc.result(new ArgResult(LoadBuiltins.string));
		context.checking(new Expectations() {{
			oneOf(sv).push(with(any(ExpressionChecker.class)));
		}});
		fc.visitFunctionIntro(fi);
		fc.result(new ExprResult(LoadBuiltins.number));
		fc.leaveFunctionIntro(fi);
		context.checking(new Expectations() {{
			oneOf(sv).result(with(ApplyMatcher.type(Matchers.is(LoadBuiltins.string), Matchers.is(LoadBuiltins.number))));
		}});
		fc.leaveFunction(fn);
	}
}
