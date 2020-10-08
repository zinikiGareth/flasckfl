package test.tc3;

import java.util.ArrayList;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.AnonymousVar;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.StandaloneMethod;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TypeBinder;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryReader;
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

import flas.matchers.ExprResultMatcher;

public class ExpressionVisitation {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	private InputPosition pos = new InputPosition("-", 1, 0, null, "hello");
	private CurrentTCState state = context.mock(CurrentTCState.class);
	private RepositoryReader repository = context.mock(RepositoryReader.class);
	private String fnCxt = "f";

	@Test
	public void numericConstantsReturnNumber() {
		NestedVisitor nv = context.mock(NestedVisitor.class);
		context.checking(new Expectations() {{
			oneOf(nv).result(with(ExprResultMatcher.expr(Matchers.is(LoadBuiltins.number))));
		}});
		ExpressionChecker tc = new ExpressionChecker(errors, repository, state, nv, fnCxt, false);
		tc.visitNumericLiteral(new NumericLiteral(pos, "42", 2));
	}

	@Test
	public void stringConstantsReturnString() {
		NestedVisitor nv = context.mock(NestedVisitor.class);
		context.checking(new Expectations() {{
			oneOf(nv).result(with(ExprResultMatcher.expr(Matchers.is(LoadBuiltins.string))));
		}});
		ExpressionChecker tc = new ExpressionChecker(errors, repository, state, nv, fnCxt, false);
		tc.visitStringLiteral(new StringLiteral(pos, "yoyo"));
	}

	@Test
	public void aNoArgConstructorReturnsItsType() {
		NestedVisitor nv = context.mock(NestedVisitor.class);
		context.checking(new Expectations() {{
			oneOf(nv).result(with(ExprResultMatcher.expr(Matchers.is(LoadBuiltins.nil))));
		}});
		TypeReference uv = new TypeReference(pos, "Nil");
		uv.bind(LoadBuiltins.nil);
		ExpressionChecker tc = new ExpressionChecker(errors, repository, state, nv, fnCxt, false);
		tc.visitTypeReference(uv, false, 0);
	}

	@Test
	public void aPreviouslyDefinedVarWithNoArgsReturnsItsType() {
		NestedVisitor nv = context.mock(NestedVisitor.class);
		Type tyX = context.mock(Type.class);
		FunctionDefinition x = new FunctionDefinition(FunctionName.function(pos, null, "x"), 0, null);
		x.bindType(tyX);
		context.checking(new Expectations() {{
			oneOf(nv).result(with(ExprResultMatcher.expr(Matchers.is(tyX))));
		}});
		UnresolvedVar uv = new UnresolvedVar(pos, "x");
		uv.bind(x);
		ExpressionChecker tc = new ExpressionChecker(errors, repository, state, nv, fnCxt, false);
		tc.visitUnresolvedVar(uv, 0);
	}

	@Test
	public void aStandaloneMethodReturnsItsResolvedType() {
		NestedVisitor nv = context.mock(NestedVisitor.class);
		Type tyX = context.mock(Type.class);
		StandaloneMethod x = new StandaloneMethod(new ObjectMethod(pos, FunctionName.standaloneMethod(pos, null, "m"), new ArrayList<>(), null, null));
		x.bindType(tyX);
		context.checking(new Expectations() {{
			oneOf(nv).result(with(ExprResultMatcher.expr(Matchers.is(tyX))));
		}});
		UnresolvedVar uv = new UnresolvedVar(pos, "x");
		uv.bind(x);
		ExpressionChecker tc = new ExpressionChecker(errors, repository, state, nv, fnCxt, false);
		tc.visitUnresolvedVar(uv, 0);
	}

	@Test
	public void aBuiltinOperatorReturnsItsType() {
		NestedVisitor nv = context.mock(NestedVisitor.class);
		Type tyPlus = context.mock(Type.class);
		FunctionDefinition plus = new FunctionDefinition(FunctionName.function(pos, null, "+"), 2, null);
		plus.bindType(tyPlus);
		context.checking(new Expectations() {{
			oneOf(nv).result(with(ExprResultMatcher.expr(Matchers.is(tyPlus))));
		}});
		UnresolvedOperator uv = new UnresolvedOperator(pos, "+");
		uv.bind(plus);
		ExpressionChecker tc = new ExpressionChecker(errors, repository, state, nv, fnCxt, false);
		tc.visitUnresolvedOperator(uv, 2);
	}

	@Test
	public void anExpressionCheckerTrampolinesResult() {
		NestedVisitor nv = context.mock(NestedVisitor.class);
		Type tyPlus = context.mock(Type.class);
		TypeBinder plus = new FunctionDefinition(FunctionName.function(pos, null, "+"), 2, null);
		plus.bindType(tyPlus);
		context.checking(new Expectations() {{
			oneOf(nv).result(with(ExprResultMatcher.expr(Matchers.is(tyPlus))));
		}});
		ExpressionChecker tc = new ExpressionChecker(errors, repository, state, nv, fnCxt, false);
		tc.result(tyPlus);
	}

	@Test
	public void aStructFieldReturnsItsType() {
		NestedVisitor nv = context.mock(NestedVisitor.class);
		StructField sf = new StructField(pos, null, false, LoadBuiltins.stringTR, "x");
		context.checking(new Expectations() {{
			oneOf(nv).result(with(ExprResultMatcher.expr(Matchers.is(LoadBuiltins.string))));
		}});
		UnresolvedVar uv = new UnresolvedVar(pos, "x");
		uv.bind(sf);
		ExpressionChecker tc = new ExpressionChecker(errors, repository, state, nv, fnCxt, false);
		tc.visitUnresolvedVar(uv, 0);
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
		ExpressionChecker tc = new ExpressionChecker(errors, repository, state, nv, fnCxt, false);
		tc.visitUnresolvedVar(uv, 0);
	}

	@Test
	public void aFunctionVariableReturnsAPolyHolder() {
		NestedVisitor nv = context.mock(NestedVisitor.class);
		FunctionName func = FunctionName.function(pos, null, "f");
		VarPattern funcVar = new VarPattern(pos, new VarName(pos, func, "x"));
		UnifiableType ut = context.mock(UnifiableType.class);
		context.checking(new Expectations() {{
			oneOf(state).requireVarConstraints(pos, fnCxt, "f.x"); will(returnValue(ut));
			oneOf(nv).result(with(ExprResultMatcher.expr(Matchers.is(ut))));
		}});
		UnresolvedVar uv = new UnresolvedVar(pos, "x");
		uv.bind(funcVar);
		ExpressionChecker tc = new ExpressionChecker(errors, repository, state, nv, fnCxt, false);
		tc.visitUnresolvedVar(uv, 0);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void anExplicitCurrySlotReturnsACurryVarType() {
		NestedVisitor nv = context.mock(NestedVisitor.class);
		context.checking(new Expectations() {{
			oneOf(nv).result(with(ExprResultMatcher.expr((Matcher)any(CurryArgumentType.class))));
		}});
		AnonymousVar uv = new AnonymousVar(pos);
		ExpressionChecker tc = new ExpressionChecker(errors, repository, state, nv, fnCxt, false);
		tc.visitAnonymousVar(uv);
	}
	
	@Test
	public void aRecursiveFunctionCallAsksTheStateForItsType() {
		NestedVisitor nv = context.mock(NestedVisitor.class);
		UnifiableType ut = context.mock(UnifiableType.class);
		FunctionName nameF = FunctionName.function(pos, null, "f");
		context.checking(new Expectations() {{
			allowing(state).getMember(nameF); will(returnValue(ut));
			oneOf(nv).result(with(ExprResultMatcher.expr(Matchers.is(ut))));
		}});
		ExpressionChecker tc = new ExpressionChecker(errors, repository, state, nv, fnCxt, false);
		UnresolvedVar uv = new UnresolvedVar(pos, "f");
		FunctionDefinition fnF = new FunctionDefinition(nameF, 1, null);
		uv.bind(fnF);
		tc.visitUnresolvedVar(uv, 0);
	}

	@Test
	public void aVarBoundToAUDDBoundToAStringReturnsString() {
		NestedVisitor nv = context.mock(NestedVisitor.class);
		context.checking(new Expectations() {{
			oneOf(nv).result(with(ExprResultMatcher.expr(Matchers.is(LoadBuiltins.string))));
		}});
		ExpressionChecker tc = new ExpressionChecker(errors, repository, state, nv, fnCxt, false);
		UnresolvedVar xx = new UnresolvedVar(pos, "x");
		UnitDataDeclaration udd = new UnitDataDeclaration(pos, false, LoadBuiltins.stringTR, FunctionName.function(pos, null, "udd"), new StringLiteral(pos, "hello"));
		xx.bind(udd);
		tc.visitUnresolvedVar(xx, 0);
	}

}
