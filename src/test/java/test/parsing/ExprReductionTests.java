package test.parsing;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parser.ExprTermConsumer;
import org.flasck.flas.parser.Punctuator;
import org.flasck.flas.parser.TDAStackReducer;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

import test.flas.testrunner.ExprMatcher;

public class ExprReductionTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	private ExprTermConsumer builder = context.mock(ExprTermConsumer.class);
	private final InputPosition pos = new InputPosition("-", 1, 0, "");
	private final TDAStackReducer reducer = new TDAStackReducer(errors, builder);

	@Test // 42
	public void aLiteralByItselfIsNotFurtherReduced() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.number(42)));
			oneOf(builder).done();
		}});
		reducer.term(new NumericLiteral(pos, "42", -1));
		reducer.done();
	}

	@Test // f 42
	public void aFunctionCallCanBeReduced() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.apply(ExprMatcher.unresolved("f"), ExprMatcher.number(42)).location("-", 1, 0, 4)));
			oneOf(builder).done();
		}});
		reducer.term(new UnresolvedVar(pos, "f"));
		reducer.term(new NumericLiteral(pos.copySetEnd(4), "42", -1));
		reducer.done();
	}

	@Test // -42
	public void aUnaryOperatorCanBeReduced() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.apply(ExprMatcher.operator("-"), ExprMatcher.number(42)).location("-", 1, 0, 3)));
			oneOf(builder).done();
		}});
		reducer.term(new UnresolvedOperator(pos, "-"));
		reducer.term(new NumericLiteral(pos.copySetEnd(3), "42", -1));
		reducer.done();
	}

	@Test // 2+4
	public void aBinaryOperatorCanBeReduced() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.apply(ExprMatcher.operator("+"), ExprMatcher.number(2), ExprMatcher.number(4)).location("-", 1, 0, 8)));
			oneOf(builder).done();
		}});
		reducer.term(new NumericLiteral(pos, "2", -1));
		reducer.term(new UnresolvedOperator(pos, "+"));
		reducer.term(new NumericLiteral(pos.copySetEnd(8), "4", -1));
		reducer.done();
	}

	@Test // 4-2
	public void binaryMinusBeReducedToo() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.apply(ExprMatcher.operator("-"), ExprMatcher.number(4), ExprMatcher.number(2)).location("-", 1, 0, 7)));
			oneOf(builder).done();
		}});
		reducer.term(new NumericLiteral(pos, "4", -1));
		reducer.term(new UnresolvedOperator(pos, "-"));
		reducer.term(new NumericLiteral(pos.copySetEnd(7), "2", -1));
		reducer.done();
	}

	@Test // 2 + f x
	public void functionToRightOfOperatorBindsTightly() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.apply(ExprMatcher.operator("+"), ExprMatcher.number(2), ExprMatcher.apply(ExprMatcher.unresolved("f"), ExprMatcher.unresolved("x"))).location("-", 1, 0, 12)));
			oneOf(builder).done();
		}});
		reducer.term(new NumericLiteral(pos, "2", -1));
		reducer.term(new UnresolvedOperator(pos, "+"));
		reducer.term(new UnresolvedVar(pos, "f"));
		reducer.term(new UnresolvedVar(pos.copySetEnd(12), "x"));
		reducer.done();
	}

	@Test // f x + 2
	public void functionToLeftOfOperatorBindsTightly() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.apply(ExprMatcher.operator("+"), ExprMatcher.apply(ExprMatcher.unresolved("f"), ExprMatcher.unresolved("x")), ExprMatcher.number(2)).location("-", 1, 0, 12)));
			oneOf(builder).done();
		}});
		reducer.term(new UnresolvedVar(pos, "f"));
		reducer.term(new UnresolvedVar(pos.copySetEnd(6), "x"));
		reducer.term(new UnresolvedOperator(pos, "+"));
		reducer.term(new NumericLiteral(pos.copySetEnd(12), "2", -1));
		reducer.done();
	}

	@Test // 2 * -3
	public void unaryOperatorBindsMoreTightlyThanMultiply() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.apply(ExprMatcher.operator("*"), ExprMatcher.number(2), ExprMatcher.apply(ExprMatcher.operator("-"), ExprMatcher.number(3))).location("-", 1, 0, 12)));
			oneOf(builder).done();
		}});
		reducer.term(new NumericLiteral(pos, "2", -1));
		reducer.term(new UnresolvedOperator(pos, "*"));
		reducer.term(new UnresolvedOperator(pos, "-"));
		reducer.term(new NumericLiteral(pos.copySetEnd(12), "3", -1));
		reducer.done();
	}

	@Test // 2 * (-3)
	public void unaryOperatorCanBePlacedInParens() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.apply(ExprMatcher.operator("*"), ExprMatcher.number(2), ExprMatcher.apply(ExprMatcher.operator("-"), ExprMatcher.number(3))).location("-", 1, 0, 12)));
			oneOf(builder).done();
		}});
		reducer.term(new NumericLiteral(pos, "2", -1));
		reducer.term(new UnresolvedOperator(pos, "*"));
		reducer.term(new Punctuator(pos, "("));
		reducer.term(new UnresolvedOperator(pos, "-"));
		reducer.term(new NumericLiteral(pos.copySetEnd(12), "3", -1));
		reducer.term(new Punctuator(pos.copySetEnd(12), ")"));
		reducer.done();
	}

	@Test // 2 + 3 + 4
	public void plusAssociatesToTheLeft() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.apply(ExprMatcher.operator("+"), ExprMatcher.number(2), ExprMatcher.apply(ExprMatcher.operator("+"), ExprMatcher.number(3), ExprMatcher.number(4))).location("-", 1, 0, 12)));
			oneOf(builder).done();
		}});
		reducer.term(new NumericLiteral(pos, "2", -1));
		reducer.term(new UnresolvedOperator(pos, "+"));
		reducer.term(new NumericLiteral(pos, "3", -1));
		reducer.term(new UnresolvedOperator(pos, "+"));
		reducer.term(new NumericLiteral(pos.copySetEnd(12), "4", -1));
		reducer.done();
	}

	@Test // 2 + 3 * 4
	public void multiplyBindsBeforePlus() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.apply(ExprMatcher.operator("+"), ExprMatcher.number(2), ExprMatcher.apply(ExprMatcher.operator("*"), ExprMatcher.number(3), ExprMatcher.number(4))).location("-", 1, 0, 12)));
			oneOf(builder).done();
		}});
		reducer.term(new NumericLiteral(pos, "2", -1));
		reducer.term(new UnresolvedOperator(pos, "+"));
		reducer.term(new NumericLiteral(pos, "3", -1));
		reducer.term(new UnresolvedOperator(pos, "*"));
		reducer.term(new NumericLiteral(pos.copySetEnd(12), "4", -1));
		reducer.done();
	}

	@Test // 2 * 3 * 4
	public void multiplyBindsBeforePlusFromTheLeftAsWell() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.apply(ExprMatcher.operator("+"), ExprMatcher.apply(ExprMatcher.operator("*"), ExprMatcher.number(2), ExprMatcher.number(3)), ExprMatcher.number(4)).location("-", 1, 0, 12)));
			oneOf(builder).done();
		}});
		reducer.term(new NumericLiteral(pos, "2", -1));
		reducer.term(new UnresolvedOperator(pos, "*"));
		reducer.term(new NumericLiteral(pos.copySetEnd(9), "3", -1));
		reducer.term(new UnresolvedOperator(pos, "+"));
		reducer.term(new NumericLiteral(pos.copySetEnd(12), "4", -1));
		reducer.done();
	}

	@Test // 2 * (3+4)
	public void parensCanMakePlusStrong() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.apply(ExprMatcher.operator("*"), ExprMatcher.number(2), ExprMatcher.apply(ExprMatcher.operator("+"), ExprMatcher.number(3), ExprMatcher.number(4))).location("-", 1, 0, 12)));
			oneOf(builder).done();
		}});
		reducer.term(new NumericLiteral(pos, "2", -1));
		reducer.term(new UnresolvedOperator(pos, "*"));
		reducer.term(new Punctuator(pos, "("));
		reducer.term(new NumericLiteral(pos, "3", -1));
		reducer.term(new UnresolvedOperator(pos, "+"));
		reducer.term(new NumericLiteral(pos.copySetEnd(8), "4", -1));
		reducer.term(new Punctuator(pos.copySetEnd(12), ")"));
		reducer.done();
	}

	@Test // a + 2*3 + b
	public void multiplyOverPlusBothWays() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.apply(ExprMatcher.operator("+"), ExprMatcher.unresolved("a"), ExprMatcher.apply(ExprMatcher.operator("+"), ExprMatcher.apply(ExprMatcher.operator("*"), ExprMatcher.number(2), ExprMatcher.number(3)), ExprMatcher.unresolved("b"))).location("-", 1, 0, 12)));
			oneOf(builder).done();
		}});
		reducer.term(new UnresolvedVar(pos, "a"));
		reducer.term(new UnresolvedOperator(pos, "+"));
		reducer.term(new NumericLiteral(pos.copySetEnd(8), "2", -1));
		reducer.term(new UnresolvedOperator(pos, "*"));
		reducer.term(new NumericLiteral(pos, "3", -1));
		reducer.term(new UnresolvedOperator(pos, "+"));
		reducer.term(new UnresolvedVar(pos.copySetEnd(12), "b"));
		reducer.done();
	}

	@Test // (a + 2)*(3 + b)
	public void parensCanOvercomeMultiply() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.apply(ExprMatcher.operator("*"), ExprMatcher.apply(ExprMatcher.operator("+"), ExprMatcher.unresolved("a"), ExprMatcher.number(2)), ExprMatcher.apply(ExprMatcher.operator("+"), ExprMatcher.number(3), ExprMatcher.unresolved("b"))).location("-", 1, 0, 12)));
			oneOf(builder).done();
		}});
		reducer.term(new Punctuator(pos, "("));
		reducer.term(new UnresolvedVar(pos, "a"));
		reducer.term(new UnresolvedOperator(pos, "+"));
		reducer.term(new NumericLiteral(pos.copySetEnd(6), "2", -1));
		reducer.term(new Punctuator(pos.copySetEnd(8), ")"));
		reducer.term(new UnresolvedOperator(pos, "*"));
		reducer.term(new Punctuator(pos, "("));
		reducer.term(new NumericLiteral(pos, "3", -1));
		reducer.term(new UnresolvedOperator(pos, "+"));
		reducer.term(new UnresolvedVar(pos.copySetEnd(10), "b"));
		reducer.term(new Punctuator(pos.copySetEnd(12), ")"));
		reducer.done();
	}
	
	@Test // 2*(3+(4-2))
	public void parensCanBeNested() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.apply(ExprMatcher.operator("*"), ExprMatcher.number(2), ExprMatcher.apply(ExprMatcher.operator("+"), ExprMatcher.number(3), ExprMatcher.apply(ExprMatcher.operator("-"), ExprMatcher.number(4), ExprMatcher.number(2)))).location("-", 1, 0, 12)));
			oneOf(builder).done();
		}});
		reducer.term(new NumericLiteral(pos, "2", -1));
		reducer.term(new UnresolvedOperator(pos, "*"));
		reducer.term(new Punctuator(pos, "("));
		reducer.term(new NumericLiteral(pos, "3", -1));
		reducer.term(new UnresolvedOperator(pos, "+"));
		reducer.term(new Punctuator(pos, "("));
		reducer.term(new NumericLiteral(pos, "4", -1));
		reducer.term(new UnresolvedOperator(pos, "-"));
		reducer.term(new NumericLiteral(pos.copySetEnd(8), "2", -1));
		reducer.term(new Punctuator(pos.copySetEnd(10), ")"));
		reducer.term(new Punctuator(pos.copySetEnd(12), ")"));
		reducer.done();
	}

	@Test // f (2*x)
	public void parensAreStrongerThanFnCall() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.apply(ExprMatcher.unresolved("f"), ExprMatcher.apply(ExprMatcher.operator("*"), ExprMatcher.number(2), ExprMatcher.unresolved("x"))).location("-", 1, 0, 12)));
			oneOf(builder).done();
		}});
		reducer.term(new UnresolvedVar(pos, "f"));
		reducer.term(new Punctuator(pos, "("));
		reducer.term(new NumericLiteral(pos.copySetEnd(6), "2", -1));
		reducer.term(new UnresolvedOperator(pos, "*"));
		reducer.term(new UnresolvedVar(pos.copySetEnd(10), "x"));
		reducer.term(new Punctuator(pos.copySetEnd(12), ")"));
		reducer.done();
	}

	@Test // 2*(f x)
	public void parensCanWrapAFnCall() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.apply(ExprMatcher.operator("*"), ExprMatcher.number(2), ExprMatcher.apply(ExprMatcher.unresolved("f"), ExprMatcher.unresolved("x"))).location("-", 1, 0, 12)));
			oneOf(builder).done();
		}});
		reducer.term(new NumericLiteral(pos, "2", -1));
		reducer.term(new UnresolvedOperator(pos, "*"));
		reducer.term(new Punctuator(pos, "("));
		reducer.term(new UnresolvedVar(pos, "f"));
		reducer.term(new UnresolvedVar(pos.copySetEnd(10), "x"));
		reducer.term(new Punctuator(pos.copySetEnd(12), ")"));
		reducer.done();
	}

	@Test // (a,b)
	public void parensCanBeUsedToCreateTuples() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.apply(ExprMatcher.operator("()"), ExprMatcher.unresolved("a"), ExprMatcher.unresolved("b")).location("-", 1, 0, 12)));
			oneOf(builder).done();
		}});
		reducer.term(new Punctuator(pos, "("));
		reducer.term(new UnresolvedVar(pos, "a"));
		reducer.term(new Punctuator(pos, ","));
		reducer.term(new UnresolvedVar(pos.copySetEnd(10), "b"));
		reducer.term(new Punctuator(pos.copySetEnd(12), ")"));
		reducer.done();
	}

	@Test // (a,b,2*c)
	public void commasActAsCloseParens() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.apply(ExprMatcher.operator("()"), ExprMatcher.unresolved("a"), ExprMatcher.unresolved("b"), ExprMatcher.apply(ExprMatcher.operator("*"), ExprMatcher.number(2), ExprMatcher.unresolved("c"))).location("-", 1, 0, 12)));
			oneOf(builder).done();
		}});
		reducer.term(new Punctuator(pos.copySetEnd(0), "("));
		reducer.term(new UnresolvedVar(pos.copySetEnd(2), "a"));
		reducer.term(new Punctuator(pos, ","));
		reducer.term(new UnresolvedVar(pos.copySetEnd(4), "b"));
		reducer.term(new Punctuator(pos, ","));
		reducer.term(new NumericLiteral(pos.copySetEnd(6), "2", -1));
		reducer.term(new UnresolvedOperator(pos.copySetEnd(7), "*"));
		reducer.term(new UnresolvedVar(pos.copySetEnd(8), "c"));
		reducer.term(new Punctuator(pos.copySetEnd(12), ")"));
		reducer.done();
	}

	@Test // [a,b]
	public void squaresCanBeUsedToCreateLists() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.apply(ExprMatcher.operator("[]"), ExprMatcher.unresolved("a"), ExprMatcher.unresolved("b")).location("-", 1, 0, 12)));
			oneOf(builder).done();
		}});
		reducer.term(new Punctuator(pos, "["));
		reducer.term(new UnresolvedVar(pos, "a"));
		reducer.term(new Punctuator(pos, ","));
		reducer.term(new UnresolvedVar(pos.copySetEnd(10), "b"));
		reducer.term(new Punctuator(pos.copySetEnd(12), "]"));
		reducer.done();
	}

	@Test // [a*b,b,2*c)
	public void commasForceElementReductionInLists() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.apply(ExprMatcher.operator("[]"), ExprMatcher.apply(ExprMatcher.operator("*"), ExprMatcher.unresolved("a"), ExprMatcher.unresolved("b")), ExprMatcher.unresolved("b"), ExprMatcher.apply(ExprMatcher.operator("*"), ExprMatcher.number(2), ExprMatcher.unresolved("c"))).location("-", 1, 0, 12)));
			oneOf(builder).done();
		}});
		reducer.term(new Punctuator(pos.copySetEnd(0), "["));
		reducer.term(new UnresolvedVar(pos.copySetEnd(2), "a"));
		reducer.term(new UnresolvedOperator(pos.copySetEnd(3), "*"));
		reducer.term(new UnresolvedVar(pos.copySetEnd(4), "b"));
		reducer.term(new Punctuator(pos, ","));
		reducer.term(new UnresolvedVar(pos.copySetEnd(5), "b"));
		reducer.term(new Punctuator(pos, ","));
		reducer.term(new NumericLiteral(pos.copySetEnd(6), "2", -1));
		reducer.term(new UnresolvedOperator(pos.copySetEnd(7), "*"));
		reducer.term(new UnresolvedVar(pos.copySetEnd(8), "c"));
		reducer.term(new Punctuator(pos.copySetEnd(12), "]"));
		reducer.done();
	}

	@Test // [a,[b,c],d)
	public void listsCanBeNested() {
		context.checking(new Expectations() {{
			oneOf(builder).term(with(ExprMatcher.apply(ExprMatcher.operator("[]"),
										ExprMatcher.unresolved("a"),
										ExprMatcher.apply(ExprMatcher.operator("[]"), ExprMatcher.unresolved("b"), ExprMatcher.unresolved("c")),
										ExprMatcher.unresolved("d")).location("-", 1, 0, 12)));
			oneOf(builder).done();
		}});
		reducer.term(new Punctuator(pos.copySetEnd(0), "["));
		reducer.term(new UnresolvedVar(pos.copySetEnd(2), "a"));
		reducer.term(new Punctuator(pos, ","));
		reducer.term(new Punctuator(pos.copySetEnd(3), "["));
		reducer.term(new UnresolvedVar(pos.copySetEnd(4), "b"));
		reducer.term(new Punctuator(pos, ","));
		reducer.term(new UnresolvedVar(pos.copySetEnd(5), "c"));
		reducer.term(new Punctuator(pos.copySetEnd(5), "]"));
		reducer.term(new Punctuator(pos, ","));
		reducer.term(new UnresolvedVar(pos.copySetEnd(8), "d"));
		reducer.term(new Punctuator(pos.copySetEnd(12), "]"));
		reducer.done();
	}

	// {a:2*4,b:f x}
	// do we have anything that associates right?

	// some error cases
	@Test // )
	public void parensCannotBeClosedIfTheyWereNeverOpened() {
		context.checking(new Expectations() {{
			oneOf(errors).message(pos, "invalid tokens after expression");
		}});
		reducer.term(new Punctuator(pos, ")"));
		reducer.done();
	}

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
