package test.tc3;

import java.util.Arrays;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.errors.LocalErrorTracker;
import org.flasck.flas.parsedForm.ut.UnitTestAssert;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.tc3.ExpressionChecker;
import org.flasck.flas.tc3.ExpressionChecker.ExprResult;
import org.flasck.flas.tc3.PolyInstance;
import org.flasck.flas.tc3.TypeChecker;
import org.flasck.flas.tc3.UTAChecker;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class UTACheckerTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private final ErrorReporter errors = context.mock(ErrorReporter.class);
	private final LocalErrorTracker tracker = new LocalErrorTracker(errors);
	private final RepositoryReader repository = context.mock(RepositoryReader.class);
	private final NestedVisitor sv = context.mock(NestedVisitor.class);
	private InputPosition pos = new InputPosition("-", 1, 0, null, "hello");
	private final String fnCxt = "ut0";

	@Test
	public void visitUTAPushesItself() {
		context.checking(new Expectations() {{
			oneOf(sv).push(with(any(TypeChecker.class)));
		}});
		TypeChecker tc = new TypeChecker(tracker, repository, sv);
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
		UTAChecker tc = new UTAChecker(errors, repository, sv, fnCxt);
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
		UTAChecker tc = new UTAChecker(errors, repository, sv, fnCxt);
		context.checking(new Expectations() {{
			oneOf(sv).push(with(any(ExpressionChecker.class)));
			oneOf(sv).push(with(any(ExpressionChecker.class)));
			oneOf(sv).result(null);
		}});
		tc.visitAssertExpr(false, uta.expr);
		tc.result(new ExprResult(pos, LoadBuiltins.string));
		tc.visitAssertExpr(true, uta.value);
		tc.result(new ExprResult(pos, LoadBuiltins.string));
		tc.postUnitTestAssert(uta);
	}
	
	@Test
	public void testUTADoesntReportAnyErrorsOnNumberAndNumber() {
		context.checking(new Expectations() {{
			oneOf(sv).push(with(any(UTAChecker.class)));
		}});
		UnitTestAssert uta = new UnitTestAssert(new StringLiteral(pos, "hello"), new StringLiteral(pos, "world"));
		UTAChecker tc = new UTAChecker(errors, repository, sv, fnCxt);
		context.checking(new Expectations() {{
			oneOf(sv).result(null);
		}});
		tc.result(new ExprResult(pos, LoadBuiltins.number));
		tc.result(new ExprResult(pos, LoadBuiltins.number));
		tc.postUnitTestAssert(uta);
	}
	
	@Test
	public void testUTACannotCompareNumberAndString() {
		context.checking(new Expectations() {{
			oneOf(sv).push(with(any(UTAChecker.class)));
		}});
		UnitTestAssert uta = new UnitTestAssert(new StringLiteral(pos, "hello"), new StringLiteral(pos, "world"));
		UTAChecker tc = new UTAChecker(errors, repository, sv, fnCxt);
		context.checking(new Expectations() {{
			oneOf(errors).message(pos, "value is of type Number that cannot be the result of an expression of type String");
			oneOf(sv).result(null);
		}});
		tc.result(new ExprResult(pos, LoadBuiltins.number));
		tc.result(new ExprResult(pos, LoadBuiltins.string));
		tc.postUnitTestAssert(uta);
	}
	
	@Test
	public void testUTACannotCompareNilAndCons() {
		context.checking(new Expectations() {{
			oneOf(sv).push(with(any(UTAChecker.class)));
		}});
		UnitTestAssert uta = new UnitTestAssert(new StringLiteral(pos, "hello"), new StringLiteral(pos, "world"));
		UTAChecker tc = new UTAChecker(errors, repository, sv, fnCxt);
		context.checking(new Expectations() {{
			oneOf(errors).message(pos, "value is of type Nil that cannot be the result of an expression of type Cons");
			oneOf(sv).result(null);
		}});
		tc.result(new ExprResult(pos, LoadBuiltins.nil));
		tc.result(new ExprResult(pos, LoadBuiltins.cons));
		tc.postUnitTestAssert(uta);
	}
	
	@Test
	public void testUTACanCompareListExpressionWithNilValue() {
		context.checking(new Expectations() {{
			oneOf(sv).push(with(any(UTAChecker.class)));
		}});
		UnitTestAssert uta = new UnitTestAssert(new StringLiteral(pos, "hello"), new StringLiteral(pos, "world"));
		UTAChecker tc = new UTAChecker(errors, repository, sv, fnCxt);
		context.checking(new Expectations() {{
			oneOf(sv).result(null);
		}});
		tc.result(new ExprResult(pos, LoadBuiltins.nil));
		tc.result(new ExprResult(pos, LoadBuiltins.list));
		tc.postUnitTestAssert(uta);
	}
	
	@Test
	public void testUTACanCompareListExpressionWithConsValueIfSamePoly() {
		context.checking(new Expectations() {{
			oneOf(sv).push(with(any(UTAChecker.class)));
		}});
		UnitTestAssert uta = new UnitTestAssert(new StringLiteral(pos, "hello"), new StringLiteral(pos, "world"));
		UTAChecker tc = new UTAChecker(errors, repository, sv, fnCxt);
		context.checking(new Expectations() {{
			oneOf(sv).result(null);
		}});
		tc.result(new ExprResult(pos, new PolyInstance(pos, LoadBuiltins.cons, Arrays.asList(LoadBuiltins.number))));
		tc.result(new ExprResult(pos, new PolyInstance(pos, LoadBuiltins.list, Arrays.asList(LoadBuiltins.number))));
		tc.postUnitTestAssert(uta);
	}
	
	@Test
	public void testUTACannotCompareListExpressionWithConsValueIfDifferentPolys() {
		context.checking(new Expectations() {{
			oneOf(sv).push(with(any(UTAChecker.class)));
		}});
		UnitTestAssert uta = new UnitTestAssert(new StringLiteral(pos, "hello"), new StringLiteral(pos, "world"));
		UTAChecker tc = new UTAChecker(errors, repository, sv, fnCxt);
		context.checking(new Expectations() {{
			oneOf(errors).message(pos, "value is of type Cons[Number] that cannot be the result of an expression of type Cons[String]");
			oneOf(sv).result(null);
		}});
		tc.result(new ExprResult(pos, new PolyInstance(pos, LoadBuiltins.cons, Arrays.asList(LoadBuiltins.number))));
		tc.result(new ExprResult(pos, new PolyInstance(pos, LoadBuiltins.cons, Arrays.asList(LoadBuiltins.string))));
		tc.postUnitTestAssert(uta);
	}
	
	@Test
	public void testUTAWillAlwaysAllowErrorValues() {
		context.checking(new Expectations() {{
			oneOf(sv).push(with(any(UTAChecker.class)));
		}});
		UnitTestAssert uta = new UnitTestAssert(new StringLiteral(pos, "hello"), new StringLiteral(pos, "world"));
		UTAChecker tc = new UTAChecker(errors, repository, sv, fnCxt);
		context.checking(new Expectations() {{
			oneOf(sv).result(null);
		}});
		tc.result(new ExprResult(pos, LoadBuiltins.error));
		tc.result(new ExprResult(pos, LoadBuiltins.number));
		tc.postUnitTestAssert(uta);
	}
	
	// The more I look at this one, the more I feel it *might* be reasonable
	// You could choose to generate a value from a function that happened to offer a bigger type
	// I think it would be OK as long as there is *some* overlap
	@Test
	public void testUTACannotCompareNilExpressionWithListValue() {
		context.checking(new Expectations() {{
			oneOf(sv).push(with(any(UTAChecker.class)));
		}});
		UnitTestAssert uta = new UnitTestAssert(new StringLiteral(pos, "hello"), new StringLiteral(pos, "world"));
		UTAChecker tc = new UTAChecker(errors, repository, sv, fnCxt);
		context.checking(new Expectations() {{
			oneOf(errors).message(pos, "value is of type List[Number] that cannot be the result of an expression of type Nil");
			oneOf(sv).result(null);
		}});
		tc.result(new ExprResult(pos, new PolyInstance(pos, LoadBuiltins.list, Arrays.asList(LoadBuiltins.number))));
		tc.result(new ExprResult(pos, LoadBuiltins.nil));
		tc.postUnitTestAssert(uta);
	}
}
