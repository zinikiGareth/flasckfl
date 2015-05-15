package org.flasck.flas.parser;

import static org.junit.Assert.assertNotNull;

import org.flasck.flas.parsedForm.ApplyExpr;
import org.flasck.flas.tokenizers.Tokenizable;
import org.junit.Test;

public class OperatorParsingTests {

	@Test
	public void testASimpleExpr() {
		Object o = new Expression().tryParsing(new Tokenizable(new StringBuilder("2*2")));
		ExprTester.assertExpr(o, "(", "*", "2", "2", ")");
	}

	@Test
	public void testAMultiplicationOfTwoFunctions() {
		Object o = new Expression().tryParsing(new Tokenizable(new StringBuilder("fib 2 * fib 3")));
		ExprTester.assertExpr(o, "(", "*", "(", "fib", "2", ")", "(", "fib", "3", ")", ")");
	}

	@Test
	public void testMultiplicationTrumpsAddition1() {
		Object o = new Expression().tryParsing(new Tokenizable(new StringBuilder("2 * 3 + 4")));
		ExprTester.assertExpr(o, "(", "+", "(", "*", "2", "3", ")", "4", ")");
	}

	@Test
	public void testMultiplicationTrumpsAddition2() {
		Object o = new Expression().tryParsing(new Tokenizable(new StringBuilder("2 + 3 * 4")));
		ExprTester.assertExpr(o, "(", "+", "2", "(", "*", "3", "4", ")", ")");
	}

	@Test
	public void testMultiplicationAssociatesLeft() {
		Object o = new Expression().tryParsing(new Tokenizable(new StringBuilder("2 * 3 * 4")));
		ExprTester.assertExpr(o, "(", "*", "(", "*", "2", "3", ")", "4", ")");
	}

	@Test
	public void testAdditionAssociatesLeft() {
		Object o = new Expression().tryParsing(new Tokenizable(new StringBuilder("2 + 3 + 4")));
		ExprTester.assertExpr(o, "(", "+", "(", "+", "2", "3", ")", "4", ")");
	}

	@Test
	public void testUnaryMinus() {
		Object o = new Expression().tryParsing(new Tokenizable(new StringBuilder("2* -x")));
		ExprTester.assertExpr(o, "(", "*", "2", "(", "-", "x", ")", ")");
	}

	@Test
	public void testUnaryMinus2() {
		Object o = new Expression().tryParsing(new Tokenizable(new StringBuilder("- 2*x")));
		ExprTester.assertExpr(o, "(", "*", "(", "-", "2", ")", "x", ")");
	}

	// TODO: we need to handle the special syntax of "_" for operator currying
	@Test
	public void testCurry1() {
		Object o = new Expression().tryParsing(new Tokenizable(new StringBuilder("map (2*_) l")));
		((ApplyExpr)o).showTree(0);
		ExprTester.assertExpr(o, "(", "map", "(", "*", "2", ")", "l", ")");
	}

	// And if we omit the left hand argument, we need to add _ to the operator name
	@Test
	public void testCurry2() {
		Object o = new Expression().tryParsing(new Tokenizable(new StringBuilder("map (_*2) l")));
		((ApplyExpr)o).showTree(0);
		ExprTester.assertExpr(o, "(", "map", "(", "*_", "2", ")", "l", ")");
	}

	// If you omit both arguments, you've basically optimized the whole thing away
	@Test
	public void testCurry3() {
		Object o = new Expression().tryParsing(new Tokenizable(new StringBuilder("map (_*_) l")));
		((ApplyExpr)o).showTree(0);
		ExprTester.assertExpr(o, "(", "map", "*", "l", ")");
	}

	// This doesn't work because we resolve a single item in parens to an ItemExpr
	// It needs to be something more special than that, probably a "ParenExpr"
	@Test
	public void testCurry4() {
		Object o = new Expression().tryParsing(new Tokenizable(new StringBuilder("map (*) l")));
		((ApplyExpr)o).showTree(0);
		ExprTester.assertExpr(o, "(", "map", "*", "l", ")");
	}
	
	@Test
	public void testEmptyList() {
		Object o = new Expression().tryParsing(new Tokenizable(new StringBuilder("[]")));
		assertNotNull(o);
		ExprTester.assertExpr(o, "[]");
	}

	@Test
	public void testSingleItemList() {
		Object o = new Expression().tryParsing(new Tokenizable(new StringBuilder("[x]")));
		assertNotNull(o);
		((ApplyExpr)o).showTree(0);
		ExprTester.assertExpr(o, "(", ":", "x", "[]", ")");
	}

	@Test
	public void testTwoItemList() {
		Object o = new Expression().tryParsing(new Tokenizable(new StringBuilder("[x,y]")));
		assertNotNull(o);
		((ApplyExpr)o).showTree(0);
		ExprTester.assertExpr(o, "(", ":", "x", "(", ":", "y", "[]", ")", ")");
	}

	@Test
	public void testTupleTwo() {
		Object o = new Expression().tryParsing(new Tokenizable(new StringBuilder("(x,y)")));
		assertNotNull(o);
		((ApplyExpr)o).showTree(0);
		ExprTester.assertExpr(o, "(", "()", "x", "y", ")");
	}

	@Test
	public void testTupleThree() {
		Object o = new Expression().tryParsing(new Tokenizable(new StringBuilder("(x,y,z)")));
		assertNotNull(o);
		((ApplyExpr)o).showTree(0);
		ExprTester.assertExpr(o, "(", "()", "x", "y", "z", ")");
	}

	// TODO: do we want to have JSON-like object syntax?
}
