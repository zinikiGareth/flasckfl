package org.flasck.flas.typechecker;

import static org.junit.Assert.*;

import org.flasck.flas.parsedForm.ItemExpr;
import org.flasck.flas.tokenizers.ExprToken;
import org.junit.Test;

public class TestBasicTypeChecking {

	@Test
	public void testWeCanTypecheckANumber() {
		TypeChecker tc = new TypeChecker();
		Object te = tc.tcExpr(new ItemExpr(new ExprToken(ExprToken.NUMBER, "1")));
		assertNotNull(te);
		assertFalse(tc.errors.hasErrors());
		assertTrue(te instanceof TypeExpr);
		TypeExpr rte = (TypeExpr) te;
		assertEquals("Number", rte.type);
		assertTrue(rte.args.isEmpty());
	}

}
