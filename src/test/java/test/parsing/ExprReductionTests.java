package test.parsing;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parser.ExprTermConsumer;
import org.flasck.flas.parser.TDAExprReducer;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

import test.flas.testrunner.ExprMatcher;

public class ExprReductionTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	private ExprTermConsumer builder = context.mock(ExprTermConsumer.class);
	private final TDAExprReducer reducer = new TDAExprReducer(errors, builder);
	private final InputPosition pos = new InputPosition("-", 1, 0, "");

	@Test
	public void aLiteralByItselfIsNotFurtherReduced() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.number(42)));
		}});
		reducer.term(new NumericLiteral(pos, "42", -1));
		reducer.done();
	}

	@Test
	public void aFunctionCallCanBeReduced() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.apply(ExprMatcher.unresolved("f"), ExprMatcher.number(42)).location("-", 1, 0, 4)));
		}});
		reducer.term(new UnresolvedVar(pos, "f"));
		reducer.term(new NumericLiteral(pos.copySetEnd(4), "42", -1));
		reducer.done();
	}

	@Test
	public void aUnaryOperatorCanBeReduced() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.apply(ExprMatcher.operator("-"), ExprMatcher.number(42)).location("-", 1, 0, 3)));
		}});
		reducer.term(new UnresolvedOperator(pos, "-"));
		reducer.term(new NumericLiteral(pos.copySetEnd(3), "42", -1));
		reducer.done();
	}

	@Test
	public void aBinaryOperatorCanBeReduced() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.apply(ExprMatcher.operator("+"), ExprMatcher.number(2), ExprMatcher.number(4)).location("-", 1, 0, 8)));
		}});
		reducer.term(new NumericLiteral(pos, "2", -1));
		reducer.term(new UnresolvedOperator(pos, "+"));
		reducer.term(new NumericLiteral(pos.copySetEnd(8), "4", -1));
		reducer.done();
	}

	// 4-2
	// 2 + f x
	// f x + 2
	// 2+3+4
	// 2+3*4
	// 2*3+4
	// f (2*x)
	// f 2 * x -- func with 3 args
	// 2*(f x)
	// 2 * f x -- what should this be? (2*f) x or 2*(f x)?
	/*
	@Test
	public void testNilBecomesAConstructorAsAnArg() {
		Object o = new Expression().tryParsing(new Tokenizable("f Nil"));
		ExprTester.assertExpr(o, "(", "f", "Nil", ")");
	}

	@Test
	public void testConsWithArgsIsNotPromoted() {
		Object o = new Expression().tryParsing(new Tokenizable("Cons head tail"));
		ExprTester.assertExpr(o, "(", "Cons", "head", "tail", ")");
	}

	// This seems like a fundamentally good plan, but I'm not quite sure what it is supposed to achieve
	@Test
	public void testTypeNilBecomesATypeValue() {
		Object o = new Expression().tryParsing(new Tokenizable("type Nil"));
		ExprTester.assertExpr(o, "Nil");
	}

	@Test
	public void testDeparenCorrectlyAssignsTokens() {
		ApplyExpr ae = (ApplyExpr) new Expression().tryParsing(new Tokenizable("(org.ziniki.Party \"Buyer\")"));
		testParty1(ae, "Buyer");
	}

	private void testParty1(ApplyExpr ae, String lit) {
		assertTrue(ae.fn instanceof ApplyExpr);
		assertEquals(1, ae.args.size());
		toklen(21 + lit.length(), ae);
		
		ApplyExpr ae1 = (ApplyExpr) ae.fn;
		assertTrue("ae1 was " + ae1.fn.getClass(), ae1.fn instanceof UnresolvedOperator);
		assertEquals(".", ((UnresolvedOperator)ae1.fn).op);
		assertEquals(2, ae1.args.size());
		toklen(1, ae1.fn);

		{
			Object aa1 = ae1.args.get(0);
			assertTrue("aa1 was " + aa1.getClass(), aa1 instanceof ApplyExpr);
			
			{
				ApplyExpr aa1e = (ApplyExpr) aa1;
				toklen(10, aa1e);
				assertTrue("aa1e was " + aa1e.fn.getClass(), ae1.fn instanceof UnresolvedOperator);
				assertEquals(".", ((UnresolvedOperator)aa1e.fn).op);
				assertEquals(2, aa1e.args.size());
				
				Object pkg = aa1e.args.get(0);
				toklen(3, pkg);
				assertTrue("pkg was " + pkg.getClass(), pkg instanceof UnresolvedVar);
				assertEquals("org", ((UnresolvedVar)pkg).var);
		
				Object subpkg = aa1e.args.get(1);
				toklen(6, subpkg);
				assertTrue("pkg was " + subpkg.getClass(), subpkg instanceof UnresolvedVar);
				assertEquals("ziniki", ((UnresolvedVar)subpkg).var);
			}
		}

		{
			Object aa2 = ae1.args.get(1);
			assertTrue("aa2 was " + aa2.getClass(), aa2 instanceof ApplyExpr);
			{
				ApplyExpr aa2e = (ApplyExpr) aa2;
				toklen(5, aa2e);
				assertTrue("aa2e was " + aa2e.fn.getClass(), aa2e.fn instanceof UnresolvedVar);
				toklen(5, aa2e.fn);
				assertEquals("Party", ((UnresolvedVar)aa2e.fn).var);
				assertEquals(0, aa2e.args.size());
			}
		}
		
		{
			Object a1 = ae.args.get(0);
			assertTrue("a1 was " + a1.getClass(), a1 instanceof StringLiteral);
			toklen(2 + lit.length(), a1);
		}
	}


	@Test
	public void testDeparenCorrectlyAssignsTokensInAList() {
		ApplyExpr ae = (ApplyExpr) new Expression().tryParsing(new Tokenizable("[(org.ziniki.Party \"Buyer\"), (org.ziniki.Party \"Seller\")]"));
		assertTrue(ae.fn instanceof UnresolvedVar);
		assertEquals("Cons", ((UnresolvedVar)ae.fn).var);
		assertEquals(2, ae.args.size());
//		toklen(61, ae);
		
		ApplyExpr ae1 = (ApplyExpr) ae.args.get(0);
		testParty1(ae1, "Buyer");

		ApplyExpr ae2 = (ApplyExpr) ae.args.get(1);
		assertTrue(ae2.fn instanceof UnresolvedVar);
		assertEquals("Cons", ((UnresolvedVar)ae2.fn).var);
		testParty1((ApplyExpr) ae2.args.get(0), "Seller");
	}

	private void toklen(int len, Object o) {
		Locatable tok = (Locatable) o;
		assertEquals(len, tok.location().pastEnd() - tok.location().off);
	}
	*/
}
