package org.flasck.flas.sampleData;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

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
		if (expected.expr instanceof ItemExpr) {
			assertTrue(actual.expr instanceof ItemExpr);
			assertItemExprsEqual((ItemExpr)expected.expr, (ItemExpr)actual.expr);
		}
	}

	private static void assertItemExprsEqual(ItemExpr eie, ItemExpr aie) {
		assertEquals(eie.tok.type, aie.tok.type);
		assertEquals(eie.tok.text, aie.tok.text);
	}
}
