package test.tc3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ConstPattern;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.patterns.HSIArgsTree;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.tc3.CurrentTCState;
import org.flasck.flas.tc3.ExpressionChecker;
import org.flasck.flas.tc3.FunctionChecker;
import org.flasck.flas.tc3.GroupChecker;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class PatternsProduceTypes {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private InputPosition pos = new InputPosition("-", 1, 0, "hello");
	private final PackageName pkg = new PackageName("test.repo");
	private final ErrorReporter errors = context.mock(ErrorReporter.class);
	private final RepositoryReader repository = context.mock(RepositoryReader.class);
	private final NestedVisitor sv = context.mock(NestedVisitor.class);
	private final CurrentTCState state = context.mock(CurrentTCState.class);
	final FunctionName nameF = FunctionName.function(pos, pkg, "fred");

	@Test
	public void aConstantPatternIsANumber() {
		context.checking(new Expectations() {{
			oneOf(sv).push(with(any(FunctionChecker.class)));
			allowing(state);
		}});
		GroupChecker tc = new GroupChecker(errors, repository, sv, state);
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
		tc.visitFunction(fn);
		tc.visitFunctionIntro(fi);
		tc.result(LoadBuiltins.number);
		tc.leaveFunctionIntro(fi);
		tc.leaveFunction(fn);
		assertNotNull("no type was bound", fn.type());
		assertEquals("Number->Number", fn.type().signature());
	}

	@Test
	public void aStringConstantPatternIsAString() {
		context.checking(new Expectations() {{
			oneOf(sv).push(with(any(ExpressionChecker.class)));
			allowing(state);
		}});
		GroupChecker tc = new GroupChecker(errors, repository, sv, state);
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
		tc.visitFunction(fn);
		tc.visitFunctionIntro(fi);
		tc.result(LoadBuiltins.number);
		tc.leaveFunctionIntro(fi);
		tc.leaveFunction(fn);
		assertNotNull("no type was bound", fn.type());
		assertEquals("String->Number", fn.type().signature());
	}
}
