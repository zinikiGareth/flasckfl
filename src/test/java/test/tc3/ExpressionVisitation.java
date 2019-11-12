package test.tc3;

import java.util.ArrayList;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.StandaloneMethod;
import org.flasck.flas.parsedForm.TypeBinder;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.tc3.CurrentTCState;
import org.flasck.flas.tc3.CurryArgumentType;
import org.flasck.flas.tc3.ExpressionChecker;
import org.flasck.flas.tc3.Type;
import org.flasck.flas.tc3.UnifiableType;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class ExpressionVisitation {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	private InputPosition pos = new InputPosition("-", 1, 0, "hello");
	private CurrentTCState state = context.mock(CurrentTCState.class);

	@Test
	public void numericConstantsReturnNumber() {
		NestedVisitor nv = context.mock(NestedVisitor.class);
		context.checking(new Expectations() {{
			oneOf(nv).result(with(ExprResultMatcher.expr(Matchers.is(LoadBuiltins.number))));
		}});
		ExpressionChecker tc = new ExpressionChecker(errors, state, nv);
		tc.visitNumericLiteral(new NumericLiteral(pos, "42", 2));
	}

	@Test
	public void stringConstantsReturnString() {
		NestedVisitor nv = context.mock(NestedVisitor.class);
		context.checking(new Expectations() {{
			oneOf(nv).result(with(ExprResultMatcher.expr(Matchers.is(LoadBuiltins.string))));
		}});
		ExpressionChecker tc = new ExpressionChecker(errors, state, nv);
		tc.visitStringLiteral(new StringLiteral(pos, "yoyo"));
	}

	@Test
	public void aNoArgConstructorReturnsItsType() {
		NestedVisitor nv = context.mock(NestedVisitor.class);
		context.checking(new Expectations() {{
			oneOf(nv).result(with(ExprResultMatcher.expr(Matchers.is(LoadBuiltins.nil))));
		}});
		UnresolvedVar uv = new UnresolvedVar(pos, "Nil");
		uv.bind(LoadBuiltins.nil);
		ExpressionChecker tc = new ExpressionChecker(errors, state, nv);
		tc.visitUnresolvedVar(uv, 0);
	}

	@Test
	public void aPreviouslyDefinedVarWithNoArgsReturnsItsType() {
		NestedVisitor nv = context.mock(NestedVisitor.class);
		Type tyX = context.mock(Type.class);
		FunctionDefinition x = new FunctionDefinition(FunctionName.function(pos, null, "x"), 0);
		x.bindType(tyX);
		context.checking(new Expectations() {{
			oneOf(nv).result(with(ExprResultMatcher.expr(Matchers.is(tyX))));
		}});
		UnresolvedVar uv = new UnresolvedVar(pos, "x");
		uv.bind(x);
		ExpressionChecker tc = new ExpressionChecker(errors, state, nv);
		tc.visitUnresolvedVar(uv, 0);
	}

	@Test
	public void aStandaloneMethodReturnsItsResolvedType() {
		NestedVisitor nv = context.mock(NestedVisitor.class);
		Type tyX = context.mock(Type.class);
		StandaloneMethod x = new StandaloneMethod(new ObjectMethod(pos, FunctionName.standaloneMethod(pos, null, "m"), new ArrayList<>()));
		x.bindType(tyX);
		context.checking(new Expectations() {{
			oneOf(nv).result(with(ExprResultMatcher.expr(Matchers.is(tyX))));
		}});
		UnresolvedVar uv = new UnresolvedVar(pos, "x");
		uv.bind(x);
		ExpressionChecker tc = new ExpressionChecker(errors, state, nv);
		tc.visitUnresolvedVar(uv, 0);
	}

	@Test
	public void aBuiltinOperatorReturnsItsType() {
		NestedVisitor nv = context.mock(NestedVisitor.class);
		Type tyPlus = context.mock(Type.class);
		FunctionDefinition plus = new FunctionDefinition(FunctionName.function(pos, null, "+"), 2);
		plus.bindType(tyPlus);
		context.checking(new Expectations() {{
			oneOf(nv).result(with(ExprResultMatcher.expr(Matchers.is(tyPlus))));
		}});
		UnresolvedOperator uv = new UnresolvedOperator(pos, "+");
		uv.bind(plus);
		ExpressionChecker tc = new ExpressionChecker(errors, state, nv);
		tc.visitUnresolvedOperator(uv, 2);
	}

	@Test
	public void anExpressionCheckerTrampolinesResult() {
		NestedVisitor nv = context.mock(NestedVisitor.class);
		Type tyPlus = context.mock(Type.class);
		TypeBinder plus = new FunctionDefinition(FunctionName.function(pos, null, "+"), 2);
		plus.bindType(tyPlus);
		context.checking(new Expectations() {{
			oneOf(nv).result(with(ExprResultMatcher.expr(Matchers.is(tyPlus))));
		}});
		ExpressionChecker tc = new ExpressionChecker(errors, state, nv);
		tc.result(tyPlus);
	}

	@Test
	public void aTypedVariableReturnsItsType() {
		NestedVisitor nv = context.mock(NestedVisitor.class);
		FunctionName func = FunctionName.function(pos, null, "f");
		TypeReference string = new TypeReference(pos, "String");
		string.bind(LoadBuiltins.string);
		TypedPattern funcVar = new TypedPattern(pos, string, new VarName(pos, func, "x"));
		context.checking(new Expectations() {{
			oneOf(nv).result(with(ExprResultMatcher.expr(Matchers.is(LoadBuiltins.string))));
		}});
		UnresolvedVar uv = new UnresolvedVar(pos, "x");
		uv.bind(funcVar);
		ExpressionChecker tc = new ExpressionChecker(errors, state, nv);
		tc.visitUnresolvedVar(uv, 0);
	}

	@Test
	public void aFunctionVariableReturnsAPolyHolder() {
		NestedVisitor nv = context.mock(NestedVisitor.class);
		FunctionName func = FunctionName.function(pos, null, "f");
		VarPattern funcVar = new VarPattern(pos, new VarName(pos, func, "x"));
		UnifiableType ut = context.mock(UnifiableType.class);
		context.checking(new Expectations() {{
			oneOf(state).requireVarConstraints(pos, "f.x"); will(returnValue(ut));
			oneOf(nv).result(with(ExprResultMatcher.expr(Matchers.is(ut))));
		}});
		UnresolvedVar uv = new UnresolvedVar(pos, "x");
		uv.bind(funcVar);
		ExpressionChecker tc = new ExpressionChecker(errors, state, nv);
		tc.visitUnresolvedVar(uv, 0);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void anExplicitCurrySlotReturnsACurryVarType() {
		NestedVisitor nv = context.mock(NestedVisitor.class);
		context.checking(new Expectations() {{
			oneOf(nv).result(with(ExprResultMatcher.expr((Matcher)any(CurryArgumentType.class))));
		}});
		UnresolvedVar uv = new UnresolvedVar(pos, "x");
		uv.bind(LoadBuiltins.ca);
		ExpressionChecker tc = new ExpressionChecker(errors, state, nv);
		tc.visitUnresolvedVar(uv, 0);
	}
	
	@Test
	public void aRecursiveFunctionCallAsksTheStateForItsType() {
		NestedVisitor nv = context.mock(NestedVisitor.class);
		UnifiableType ut = context.mock(UnifiableType.class);
		context.checking(new Expectations() {{
			oneOf(state).requireVarConstraints(null, "f"); will(returnValue(ut));
			oneOf(nv).result(with(ExprResultMatcher.expr(Matchers.is(ut))));
		}});
		ExpressionChecker tc = new ExpressionChecker(errors, state, nv);
		UnresolvedVar uv = new UnresolvedVar(pos, "f");
		FunctionDefinition fnF = new FunctionDefinition(FunctionName.function(pos, null, "f"), 1);
		uv.bind(fnF);
		tc.visitUnresolvedVar(uv, 0);
	}
}
