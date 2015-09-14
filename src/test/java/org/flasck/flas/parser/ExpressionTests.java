package org.flasck.flas.parser;

import org.flasck.flas.tokenizers.Tokenizable;
import org.junit.Ignore;
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

}
