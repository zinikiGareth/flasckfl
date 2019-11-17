package test.tc3;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ut.UnitTestAssert;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.tc3.ExpressionChecker;
import org.flasck.flas.tc3.TypeChecker;
import org.flasck.flas.tc3.UTAChecker;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class UTACheckerTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private final ErrorReporter errors = context.mock(ErrorReporter.class);
	private final RepositoryReader repository = context.mock(RepositoryReader.class);
	private final NestedVisitor sv = context.mock(NestedVisitor.class);
	private InputPosition pos = new InputPosition("-", 1, 0, "hello");
	private final PackageName pkg = new PackageName("test.repo");

	@Test
	public void visitUTAPushesItself() {
		context.checking(new Expectations() {{
			oneOf(sv).push(with(any(TypeChecker.class)));
		}});
		TypeChecker tc = new TypeChecker(errors, repository, sv);
		context.checking(new Expectations() {{
			oneOf(sv).push(with(any(UTAChecker.class)));
		}});
		UnitTestAssert uta = new UnitTestAssert(new StringLiteral(pos, "hello"), new StringLiteral(pos, "world"));
		tc.visitUnitTestAssert(uta);
	}
	
	@Test
	public void testUTACheckerPushesExprCheckerOnVisitExpr() {
		context.checking(new Expectations() {{
			oneOf(sv).push(with(any(UTAChecker.class)));
		}});
		UnitTestAssert uta = new UnitTestAssert(new StringLiteral(pos, "hello"), new StringLiteral(pos, "world"));
		UTAChecker tc = new UTAChecker(errors, repository, sv, uta);
		context.checking(new Expectations() {{
			oneOf(sv).push(with(any(ExpressionChecker.class)));
		}});
		tc.visitAssertExpr(false, uta.expr);
	}
	
	@Test
	public void testUTACollectsTwoExpressions() {
		context.checking(new Expectations() {{
			oneOf(sv).push(with(any(UTAChecker.class)));
		}});
		UnitTestAssert uta = new UnitTestAssert(new StringLiteral(pos, "hello"), new StringLiteral(pos, "world"));
		UTAChecker tc = new UTAChecker(errors, repository, sv, uta);
		context.checking(new Expectations() {{
			oneOf(sv).push(with(any(ExpressionChecker.class)));
			oneOf(sv).push(with(any(ExpressionChecker.class)));
			oneOf(sv).result(null);
		}});
		tc.visitAssertExpr(false, uta.expr);
		tc.result(null);
		tc.visitAssertExpr(true, uta.value);
		tc.result(null);
		tc.postUnitTestAssert(uta);
	}
}
