package test.tc3;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.parsedForm.FieldsDefn.FieldsType;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.WithTypeSignature;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.tc3.ExpressionChecker;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class ExpressionVisitation {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private InputPosition pos = new InputPosition("-", 1, 0, "hello");

	@Test
	public void numericConstantsReturnNumber() {
		RepositoryReader repository = context.mock(RepositoryReader.class);
		NestedVisitor nv = context.mock(NestedVisitor.class);
		RepositoryEntry tyNumber = context.mock(RepositoryEntry.class);
		context.checking(new Expectations() {{
			oneOf(repository).get("Number"); will(returnValue(tyNumber));
			oneOf(nv).result(tyNumber);
		}});
		ExpressionChecker tc = new ExpressionChecker(repository, nv);
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
		ExpressionChecker tc = new ExpressionChecker(repository, nv);
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
		ExpressionChecker tc = new ExpressionChecker(repository, nv);
		tc.visitUnresolvedVar(uv, 0);
	}

	@Test
	public void aPreviouslyDefinedVarWithNoArgsReturnsItsType() {
		RepositoryReader repository = context.mock(RepositoryReader.class);
		NestedVisitor nv = context.mock(NestedVisitor.class);
		WithTypeSignature tyX = context.mock(WithTypeSignature.class);
		FunctionDefinition x = new FunctionDefinition(FunctionName.function(pos, null, "x"), 0);
		x.bindType(tyX);
		context.checking(new Expectations() {{
			oneOf(nv).result(tyX);
		}});
		UnresolvedVar uv = new UnresolvedVar(pos, "x");
		uv.bind(x);
		ExpressionChecker tc = new ExpressionChecker(repository, nv);
		tc.visitUnresolvedVar(uv, 0);
	}
}
