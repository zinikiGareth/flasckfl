package org.flasck.flas.sampleData;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.ConstPattern;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.parser.ItemExpr;
import org.flasck.flas.tokenizers.ExprToken;

public class ParsedFormTestData {
	static InputPosition posn = new InputPosition("test", 1, 1, null);

	public static FunctionCaseDefn fibDefn1() {
		List<Object> args = new ArrayList<Object>();
		args.add(new ConstPattern(posn, ConstPattern.INTEGER, "0"));
		Object ie = ItemExpr.from(new ExprToken(posn, ExprToken.NUMBER, "1"));
		return new FunctionCaseDefn(FunctionName.function(posn, null, "fib"), args, ie);
	}

	public static FunctionCaseDefn fibDefn2() {
		List<Object> args = new ArrayList<Object>();
		args.add(new ConstPattern(posn, ConstPattern.INTEGER, "1"));
		Object ie = ItemExpr.from(new ExprToken(posn, ExprToken.NUMBER, "1"));
		return new FunctionCaseDefn(FunctionName.function(posn, null, "fib"), args, ie);
	}

	public static FunctionCaseDefn fibDefnN() {
		List<Object> args = new ArrayList<Object>();
		args.add(new VarPattern(null, "n"));
		
		ApplyExpr minus1 = new ApplyExpr(posn, se("-"), ie("n"), ne("1"));
		ApplyExpr lhs = new ApplyExpr(posn, ie("fib"), minus1);
		ApplyExpr minus2 = new ApplyExpr(posn, se("-"), ie("n"), ne("2"));
		ApplyExpr rhs = new ApplyExpr(posn, ie("fib"), minus2);
		ApplyExpr top = new ApplyExpr(posn, se("+"), lhs, rhs);
		
		return new FunctionCaseDefn(FunctionName.function(posn, null, "fib"), args, top);
	}

	private static Object ie(String tok) {
		return ItemExpr.from(new ExprToken(posn, ExprToken.IDENTIFIER, tok));
	}

	private static Object ne(String tok) {
		return ItemExpr.from(new ExprToken(posn, ExprToken.NUMBER, tok));
	}

	private static Object se(String tok) {
		return ItemExpr.from(new ExprToken(posn, ExprToken.SYMBOL, tok));
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
		assertEquals(expected.intro.name().uniqueName(), actual.intro.name().uniqueName());
		assertEquals(expected.intro.args.size(), actual.intro.args.size());
		for (int i=0;i<expected.intro.args.size();i++)
			assertPatternsEqual(expected.intro.args.get(i), actual.intro.args.get(i));
		assertExprsEqual(expected.expr, actual.expr);
	}

	private static void assertPatternsEqual(Object expected, Object actual) {
		if (expected instanceof ConstPattern) {
			assertTrue(actual instanceof ConstPattern);
			assertEquals(((ConstPattern)expected).type, ((ConstPattern)actual).type);
			assertEquals(((ConstPattern)expected).value, ((ConstPattern)actual).value);
		} else if (expected instanceof VarPattern) {
			assertTrue(actual instanceof VarPattern);
//			assertEquals(((VarPattern)expected).type, ((VarPattern)actual).type);
			assertEquals(((VarPattern)expected).var, ((VarPattern)actual).var);
		} else
			fail("Don't understand " + expected.getClass());
	}

	private static void assertExprsEqual(Object expected, Object actual) {
		if (expected instanceof NumericLiteral || expected instanceof UnresolvedOperator || expected instanceof UnresolvedVar) {
			assertEquals(expected.toString(), actual.toString());
		} else if (expected instanceof ApplyExpr) {
			assertTrue(actual instanceof ApplyExpr);
			assertApplyExprsEqual((ApplyExpr)expected, (ApplyExpr)actual);
		} else
			fail("Cannot handle expr of type " + expected.getClass());
	}

	private static void assertApplyExprsEqual(ApplyExpr eae, ApplyExpr aae) {
		assertExprsEqual(eae.fn, aae.fn);
		assertEquals(eae.args.size(), aae.args.size());
		for (int i=0;i<eae.args.size();i++)
			assertExprsEqual(eae.args.get(i), aae.args.get(i));
	}
}
