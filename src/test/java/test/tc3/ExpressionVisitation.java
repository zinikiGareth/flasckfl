package test.tc3;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FieldsDefn.FieldsType;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.tc3.CurrentTCState;
import org.flasck.flas.tc3.ExpressionChecker;
import org.flasck.flas.tc3.Type;
import org.flasck.flas.tc3.UnifiableType;
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
		RepositoryReader repository = context.mock(RepositoryReader.class);
		NestedVisitor nv = context.mock(NestedVisitor.class);
		RepositoryEntry tyNumber = context.mock(RepositoryEntry.class);
		context.checking(new Expectations() {{
			oneOf(repository).get("Number"); will(returnValue(tyNumber));
			oneOf(nv).result(tyNumber);
		}});
		ExpressionChecker tc = new ExpressionChecker(errors, repository, state, nv);
		tc.visitNumericLiteral(new NumericLiteral(pos, "42", 2));
	}

	@Test
	public void stringConstantsReturnString() {
		RepositoryReader repository = context.mock(RepositoryReader.class);
		NestedVisitor nv = context.mock(NestedVisitor.class);
		RepositoryEntry tyNumber = context.mock(RepositoryEntry.class);
		context.checking(new Expectations() {{
			oneOf(repository).get("String"); will(returnValue(tyNumber));
			oneOf(nv).result(tyNumber);
		}});
		ExpressionChecker tc = new ExpressionChecker(errors, repository, state, nv);
		tc.visitStringLiteral(new StringLiteral(pos, "yoyo"));
	}

	@Test
	public void aNoArgConstructorReturnsItsType() {
		RepositoryReader repository = context.mock(RepositoryReader.class);
		NestedVisitor nv = context.mock(NestedVisitor.class);
		RepositoryEntry tyNil = new StructDefn(pos, FieldsType.STRUCT, null, "Nil", false);
		context.checking(new Expectations() {{
			oneOf(nv).result(tyNil);
		}});
		UnresolvedVar uv = new UnresolvedVar(pos, "Nil");
		uv.bind(tyNil);
		ExpressionChecker tc = new ExpressionChecker(errors, repository, state, nv);
		tc.visitUnresolvedVar(uv, 0);
	}

	@Test
	public void aPreviouslyDefinedVarWithNoArgsReturnsItsType() {
		RepositoryReader repository = context.mock(RepositoryReader.class);
		NestedVisitor nv = context.mock(NestedVisitor.class);
		Type tyX = context.mock(Type.class);
		FunctionDefinition x = new FunctionDefinition(FunctionName.function(pos, null, "x"), 0);
		x.bindType(tyX);
		context.checking(new Expectations() {{
			oneOf(nv).result(tyX);
		}});
		UnresolvedVar uv = new UnresolvedVar(pos, "x");
		uv.bind(x);
		ExpressionChecker tc = new ExpressionChecker(errors, repository, state, nv);
		tc.visitUnresolvedVar(uv, 0);
	}

	@Test
	public void aBuiltinOperatorReturnsItsType() {
		RepositoryReader repository = context.mock(RepositoryReader.class);
		NestedVisitor nv = context.mock(NestedVisitor.class);
		Type tyPlus = context.mock(Type.class);
		FunctionDefinition plus = new FunctionDefinition(FunctionName.function(pos, null, "+"), 2);
		plus.bindType(tyPlus);
		context.checking(new Expectations() {{
			oneOf(nv).result(tyPlus);
		}});
		UnresolvedOperator uv = new UnresolvedOperator(pos, "+");
		uv.bind(plus);
		ExpressionChecker tc = new ExpressionChecker(errors, repository, state, nv);
		tc.visitUnresolvedOperator(uv, 2);
	}

	@Test
	public void anExpressionCheckerTrampolinesResult() {
		RepositoryReader repository = context.mock(RepositoryReader.class);
		NestedVisitor nv = context.mock(NestedVisitor.class);
		Type tyPlus = context.mock(Type.class);
		FunctionDefinition plus = new FunctionDefinition(FunctionName.function(pos, null, "+"), 2);
		plus.bindType(tyPlus);
		context.checking(new Expectations() {{
			oneOf(nv).result(tyPlus);
		}});
		ExpressionChecker tc = new ExpressionChecker(errors, repository, state, nv);
		tc.result(tyPlus);
	}

	@Test
	public void aTypedVariableReturnsItsType() {
		RepositoryReader repository = context.mock(RepositoryReader.class);
		NestedVisitor nv = context.mock(NestedVisitor.class);
		FunctionName func = FunctionName.function(pos, null, "f");
		TypeReference string = new TypeReference(pos, "String");
		string.bind(LoadBuiltins.string);
		TypedPattern funcVar = new TypedPattern(pos, string, new VarName(pos, func, "x"));
		context.checking(new Expectations() {{
			oneOf(nv).result(LoadBuiltins.string);
		}});
		UnresolvedVar uv = new UnresolvedVar(pos, "x");
		uv.bind(funcVar);
		ExpressionChecker tc = new ExpressionChecker(errors, repository, state, nv);
		tc.visitUnresolvedVar(uv, 0);
	}

	@Test
	public void aFunctionVariableReturnsAPolyHolder() {
		RepositoryReader repository = context.mock(RepositoryReader.class);
		NestedVisitor nv = context.mock(NestedVisitor.class);
		FunctionName func = FunctionName.function(pos, null, "f");
		VarPattern funcVar = new VarPattern(pos, new VarName(pos, func, "x"));
		UnifiableType ut = context.mock(UnifiableType.class);
		context.checking(new Expectations() {{
			oneOf(state).requireVarConstraints(pos, "f.x"); will(returnValue(ut));
			oneOf(nv).result(ut);
		}});
		UnresolvedVar uv = new UnresolvedVar(pos, "x");
		uv.bind(funcVar);
		ExpressionChecker tc = new ExpressionChecker(errors, repository, state, nv);
		tc.visitUnresolvedVar(uv, 0);
	}
}
