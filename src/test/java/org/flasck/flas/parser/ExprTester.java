package org.flasck.flas.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.flasck.flas.parsedForm.ApplyExpr;
import org.flasck.flas.parsedForm.ItemExpr;

public class ExprTester {

	public static void assertExpr(Object o, String... patt) {
		int k = assertExprRec(o, patt, 0);
		assertEquals(patt.length, k);
	}

	private static int assertExprRec(Object o, String[] patt, int pos) {
		if (patt[pos].equals("(")) {
			pos++;
			assertTrue(o instanceof ApplyExpr);
			ApplyExpr ae = (ApplyExpr) o;
			pos = assertExprRec(ae.fn, patt, pos);
			int k = 0;
			while (patt[pos] != ")") {
				assertTrue(ae.args.size() > k);
				pos = assertExprRec(ae.args.get(k), patt, pos);
				k++;
			}
			assertEquals(k, ae.args.size());
			return pos+1;
		} else {
			assertTrue(o instanceof ItemExpr);
			assertEquals(patt[pos], ((ItemExpr)o).tok.text);
			return pos+1;
		}
		
	}

}
