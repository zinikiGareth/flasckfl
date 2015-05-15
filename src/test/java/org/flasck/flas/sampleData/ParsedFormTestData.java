package org.flasck.flas.sampleData;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.parsedForm.ApplyExpr;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.ItemExpr;
import org.flasck.flas.tokenizers.ExprToken;

public class ParsedFormTestData {
	public static FunctionCaseDefn fibDefn1() {
		List<Object> args = new ArrayList<Object>();
		args.add("0");
		ItemExpr ie = new ItemExpr(new ExprToken(ExprToken.NUMBER, "1"));
		return new FunctionCaseDefn("fib", args, ie);
	}

	public static FunctionCaseDefn fibDefn2() {
		List<Object> args = new ArrayList<Object>();
		args.add("1");
		ItemExpr ie = new ItemExpr(new ExprToken(ExprToken.NUMBER, "1"));
		return new FunctionCaseDefn("fib", args, ie);
	}

	public static FunctionCaseDefn fibDefnN() {
		List<Object> args = new ArrayList<Object>();
		args.add("n");
		
		ApplyExpr minus1 = new ApplyExpr(se("-"), ie("n"), ne("1"));
		ApplyExpr lhs = new ApplyExpr(ie("fib"), minus1);
		ApplyExpr minus2 = new ApplyExpr(se("-"), ie("n"), ne("2"));
		ApplyExpr rhs = new ApplyExpr(ie("fib"), minus2);
		ApplyExpr top = new ApplyExpr(se("+"), lhs, rhs);
		
		return new FunctionCaseDefn("fib", args, top);
	}

	private static ItemExpr ie(String tok) {
		return new ItemExpr(new ExprToken(ExprToken.IDENTIFIER, tok));
	}

	private static ItemExpr ne(String tok) {
		return new ItemExpr(new ExprToken(ExprToken.NUMBER, tok));
	}

	private static ItemExpr se(String tok) {
		return new ItemExpr(new ExprToken(ExprToken.SYMBOL, tok));
	}

	public static void assertFormsEqual(Object expected, Object actual) {
		assertNotNull(actual);
		if (expected instanceof FunctionCaseDefn) {
			assertTrue(actual instanceof FunctionCaseDefn);
			assertFCDsEqual((FunctionCaseDefn) expected, (FunctionCaseDefn)actual);
		}
		else
			fail("Cannot handle expected " + expected.getClass());
	}

	private static void assertFCDsEqual(FunctionCaseDefn expected, FunctionCaseDefn actual) {
		assertEquals(expected.name, actual.name);
		assertEquals(expected.args.size(), actual.args.size());
		for (int i=0;i<expected.args.size();i++)
			assertEquals(expected.args.get(i), actual.args.get(i));
		assertExprsEqual(expected.expr, actual.expr);
	}

	private static void assertExprsEqual(Object expected, Object actual) {
		if (expected instanceof ItemExpr) {
			assertTrue(actual instanceof ItemExpr);
			assertItemExprsEqual((ItemExpr)expected, (ItemExpr)actual);
		} else if (expected instanceof ApplyExpr) {
			assertTrue(actual instanceof ApplyExpr);
			assertApplyExprsEqual((ApplyExpr)expected, (ApplyExpr)actual);
		} else
			fail("Cannot handle expr of type " + expected.getClass());
	}

	private static void assertItemExprsEqual(ItemExpr eie, ItemExpr aie) {
		assertEquals(eie.tok.type, aie.tok.type);
		assertEquals(eie.tok.text, aie.tok.text);
	}

	private static void assertApplyExprsEqual(ApplyExpr eae, ApplyExpr aae) {
		assertExprsEqual(eae.fn, aae.fn);
		assertEquals(eae.args.size(), aae.args.size());
		for (int i=0;i<eae.args.size();i++)
			assertExprsEqual(eae.args.get(i), aae.args.get(i));
	}
}
