package test.tc3;

import java.util.ArrayList;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.tc3.CurrentTCState;
import org.flasck.flas.tc3.ExpressionChecker.ExprResult;
import org.flasck.flas.tc3.FunctionChecker;
import org.flasck.flas.tc3.FunctionChecker.ArgResult;
import org.hamcrest.Matchers;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import flas.matchers.ApplyMatcher;
import flas.matchers.ConsolidatedTypeMatcher;

public class TypeConsolidation {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private final ErrorReporter errors = context.mock(ErrorReporter.class);
	private final CurrentTCState state = context.mock(CurrentTCState.class);
	private final NestedVisitor nv = context.mock(NestedVisitor.class);
	private InputPosition pos = new InputPosition("-", 1, 0, "hello");
	FunctionDefinition f = new FunctionDefinition(FunctionName.function(pos, null, "f"), 0);
	FunctionIntro fi = new FunctionIntro(f.name(), new ArrayList<Pattern>());

	@Before
	public void before() {
		f.intro(fi);
	}
	
	@Test
	public void withNoArgsItsJustASimpleConstant() {
		FunctionChecker fc = new FunctionChecker(errors, nv, state);
		fc.result(new ExprResult(LoadBuiltins.number));
		
		context.checking(new Expectations() {{
			oneOf(nv).result(LoadBuiltins.number);
		}});
		fc.leaveFunction(f);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void anArgAndAResultImpliesAnApply() {
		FunctionChecker fc = new FunctionChecker(errors, nv, state);
		fc.result(new ArgResult(LoadBuiltins.nil));
		fc.result(new ExprResult(LoadBuiltins.number));
		
		context.checking(new Expectations() {{
			oneOf(nv).result(with(ApplyMatcher.type(Matchers.is(LoadBuiltins.nil), Matchers.is(LoadBuiltins.number))));
		}});
		fc.leaveFunction(f);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void multipleIdenticallyTypedExpressionsCanBeConsolidatedInAContainer() {
		FunctionChecker fc = new FunctionChecker(errors, nv, state);
		fc.result(new ExprResult(LoadBuiltins.number));
		fc.result(new ExprResult(LoadBuiltins.number));
		
		context.checking(new Expectations() {{
			oneOf(nv).result(with(ConsolidatedTypeMatcher.with(Matchers.is(LoadBuiltins.number), Matchers.is(LoadBuiltins.number))));
		}});
		fc.leaveFunction(f);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void multipleExpressionsCanBeConsolidatedInAContainer() {
		FunctionChecker fc = new FunctionChecker(errors, nv, state);
		fc.result(new ExprResult(LoadBuiltins.trueT));
		fc.result(new ExprResult(LoadBuiltins.falseT));
		
		context.checking(new Expectations() {{
			oneOf(nv).result(with(ConsolidatedTypeMatcher.with(Matchers.is(LoadBuiltins.trueT), Matchers.is(LoadBuiltins.falseT))));
		}});
		fc.leaveFunction(f);
	}
}
