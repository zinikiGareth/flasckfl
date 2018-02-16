package org.flasck.flas.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.tokenizers.Tokenizable;
import org.junit.Test;

public class ExpressionTests {

	@Test
	public void testVarIsParsedAsAnUnresolvedVar() {
		Object o = new Expression().tryParsing(new Tokenizable("x"));
		ExprTester.assertExpr(o, "x");
	}

	@Test
	public void testNilBecomesAConstructorAsAValue() {
		Object o = new Expression().tryParsing(new Tokenizable("Nil"));
		ExprTester.assertExpr(o, "Nil");
	}

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
}
