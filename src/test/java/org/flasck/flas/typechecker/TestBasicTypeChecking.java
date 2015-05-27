package org.flasck.flas.typechecker;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.flasck.flas.hsie.HSIETestData;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.junit.Test;
import org.zinutils.collections.CollectionUtils;

public class TestBasicTypeChecking {

	@Test
	public void testWeCanTypecheckANumber() {
		TypeChecker tc = new TypeChecker(new ArrayList<HSIEForm>());
		Object te = tc.tcExpr(HSIETestData.simpleFn().nestedCommands().get(0));
		assertNotNull(te);
		assertFalse(tc.errors.hasErrors());
		assertTrue(te instanceof TypeExpr);
		TypeExpr rte = (TypeExpr) te;
		assertEquals("Number", rte.type);
		assertTrue(rte.args.isEmpty());
	}

	@Test
	public void testWeCanTypecheckAVerySimpleLambda() {
		TypeChecker tc = new TypeChecker(CollectionUtils.listOf(HSIETestData.simpleFn()));
		tc.typecheck();
//		assertNotNull(te);
		assertFalse(tc.errors.hasErrors());
//		assertTrue(te instanceof TypeExpr);
//		TypeExpr rte = (TypeExpr) te;
//		assertEquals("Number", rte.type);
//		assertTrue(rte.args.isEmpty());
	}
}
